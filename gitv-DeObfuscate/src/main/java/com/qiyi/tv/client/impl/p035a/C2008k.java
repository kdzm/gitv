package com.qiyi.tv.client.impl.p035a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.qiyi.tv.client.impl.Command;
import com.qiyi.tv.client.impl.ParamsHelper;
import com.qiyi.tv.client.impl.Utils;

final class C2008k extends Command {
    public C2008k(Context context) {
        super(context, 10000, 20000, 30000);
    }

    public final Bundle process(Bundle params) {
        Utils.startActivity(getContext(), (Intent) ParamsHelper.parseResultData(params));
        return Utils.createResultBundle(0);
    }
}
