package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.DocumentSessionMapper;
import cn.wyq.serverwebsocket.mapper.SysDocumentMapper;
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

@Service
@RequiredArgsConstructor
public class DocumentSessionService {

    private static final int MAX_DOCUMENTS_PER_SESSION = 20;

    private final DocumentSessionMapper documentSessionMapper;
    private final SysDocumentMapper sysDocumentMapper;
    private final CacheClient cacheClient;

    public void assertOwned(String sessionId, String userName) {
        if (!StringUtils.hasText(sessionId) || !sessionId.startsWith("document_")
                || !StringUtils.hasText(userName)
                || documentSessionMapper.countOwnedSession(sessionId, userName) == 0) {
            throw new ServiceException("无权访问该文档会话", 403);
        }
    }

    public List<String> listDocumentIds(String sessionId, String userName) {
        assertOwned(sessionId, userName);
        return cacheClient.queryWithMutex("ai:document-session", sessionId,
                new TypeReference<List<String>>() {},
                ignored -> {
                    List<String> ids = documentSessionMapper.selectDocumentIds(sessionId);
                    return ids == null ? List.of() : ids;
                }, 2, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<String> replaceDocuments(String sessionId, String userName, List<String> documentIds) {
        assertOwned(sessionId, userName);

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
            if (sysDocumentMapper.selectById(documentId) == null) {
                throw new ServiceException("文档不存在或已被删除: " + documentId, 400);
            }
        }

        documentSessionMapper.deleteBySessionId(sessionId);
        for (String documentId : normalizedIds) {
            documentSessionMapper.insertAssociation(sessionId, documentId);
        }
        cacheClient.evict("ai:document-session", sessionId);
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
            cacheClient.evict("ai:document-session", sessionId);
        }
    }
}
