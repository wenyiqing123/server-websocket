package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-06T14:20:30+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class MessageStructMapperImpl implements MessageStructMapper {

    @Override
    public MessageFull toFull(MessageInfo info) {
        if ( info == null ) {
            return null;
        }

        MessageFull messageFull = new MessageFull();

        messageFull.setId( info.getId() );
        messageFull.setMessage( info.getMessage() );
        messageFull.setFromName( info.getFromName() );
        messageFull.setTime( info.getTime() );

        return messageFull;
    }

    @Override
    public List<MessageFull> toFullList(List<MessageInfo> infoList) {
        if ( infoList == null ) {
            return null;
        }

        List<MessageFull> list = new ArrayList<MessageFull>( infoList.size() );
        for ( MessageInfo messageInfo : infoList ) {
            list.add( toFull( messageInfo ) );
        }

        return list;
    }
}
