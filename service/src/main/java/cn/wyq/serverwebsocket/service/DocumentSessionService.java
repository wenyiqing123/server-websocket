package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.DocumentSessionMapper;
import cn.wyq.serverwebsocket.mapper.SysDocumentMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DocumentSessionService：业务服务组件，提供可复用的领域处理能力。
 */
@Service
@RequiredArgsConstructor
public class DocumentSessionService {

    private static final int MAX_DOCUMENTS_PER_SESSION = 20;

    private final DocumentSessionMapper documentSessionMapper;
    private final SysDocumentMapper sysDocumentMapper;
    private final CacheClient cacheClient;

    public void assertOwned(String sessionId, Integer ownerUserId) {
        if (!StringUtils.hasText(sessionId) || !sessionId.startsWith("document_")
                || ownerUserId == null || ownerUserId <= 0
                || documentSessionMapper.countOwnedSession(sessionId, ownerUserId) == 0) {
            throw new ServiceException("无权访问该文档会话", 403);
        }
    }

    public List<String> listDocumentIds(String sessionId, Integer ownerUserId) {
        assertOwned(sessionId, ownerUserId);
        validateDocumentOwner(ownerUserId);
        String cacheId = cacheId(sessionId, ownerUserId);
        return cacheClient.queryWithMutex("ai:document-session", cacheId,
                new TypeReference<List<String>>() {},
                ignored -> {
                    List<String> ids = documentSessionMapper.selectDocumentIds(sessionId, ownerUserId);
                    return ids == null ? List.of() : ids;
                }, 2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<String> replaceDocuments(String sessionId, Integer ownerUserId,
                                         List<String> documentIds) {
        assertOwned(sessionId, ownerUserId);
        validateDocumentOwner(ownerUserId);

        Set<String> normalizedIds = new LinkedHashSet<>();
        if (documentIds != null) {
            for (String documentId : documentIds) {
                if (StringUtils.hasText(documentId)) {
                    normalizedIds.add(documentId.trim());
                }
            }
        }

        if (normalizedIds.size() > MAX_DOCUMENTS_PER_SESSION) {
            throw new ServiceException("单个会话最多关联 " + MAX_DOCUMENTS_PER_SESSION + " 个文档", 400);
        }

        for (String documentId : normalizedIds) {
            SysDocument document = sysDocumentMapper.selectById(documentId);
            if (document == null || !ownerUserId.equals(document.getOwnerUserId())) {
                throw new ServiceException("文档不存在或无权访问: " + documentId, 403);
            }
        }

        documentSessionMapper.deleteBySessionId(sessionId);
        for (String documentId : normalizedIds) {
            documentSessionMapper.insertAssociation(sessionId, documentId);
        }
        cacheClient.evict("ai:document-session", cacheId(sessionId, ownerUserId));
        return new ArrayList<>(normalizedIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeDocument(String documentId) {
        if (StringUtils.hasText(documentId)) {
            documentSessionMapper.deleteByDocumentId(documentId);
            cacheClient.evictByPrefix("ai:document-session");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSession(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            documentSessionMapper.deleteBySessionId(sessionId);
            cacheClient.evictByPrefix("ai:document-session");
        }
    }

    private void validateDocumentOwner(Integer ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new ServiceException("请先登录", 401);
        }
    }

    private String cacheId(String sessionId, Integer ownerUserId) {
        return ownerUserId + ":" + sessionId;
    }
}
