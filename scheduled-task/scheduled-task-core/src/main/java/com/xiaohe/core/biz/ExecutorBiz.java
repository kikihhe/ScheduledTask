package com.xiaohe.core.biz;

import com.xiaohe.core.model.*;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-09-30 19:37
 */
public interface ExecutorBiz {
    public Result beat();

    public Result idleBeat(IdleBeatParam idleBeatParam);

    public Result run(TriggerParam triggerParam);

    public Result kill(KillParam killParam);

    public Result log(LogParam logParam);
}
