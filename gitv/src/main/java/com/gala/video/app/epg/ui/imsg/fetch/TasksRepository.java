package com.gala.video.app.epg.ui.imsg.fetch;

import com.gala.albumprovider.model.Tag;
import com.gala.video.lib.framework.core.utils.ThreadUtils;
import com.gala.video.lib.share.ifmanager.bussnessIF.imsg.IMsgContent;
import java.util.List;

public class TasksRepository implements IMsgDataSource {
    private IMsgDataSource mMsgDataSource = new MsgDataSource();

    public void getMsgList(final int type, final IMsgCallback callback) {
        ThreadUtils.execute(new Runnable() {
            public void run() {
                TasksRepository.this.mMsgDataSource.getMsgList(type, new IMsgCallback() {
                    public void onSuccess(List<IMsgContent> list) {
                        callback.onSuccess(list);
                    }

                    public void onFail() {
                        callback.onFail();
                    }
                });
            }
        });
    }

    public List<Tag> getLabels() {
        return this.mMsgDataSource.getLabels();
    }
}
