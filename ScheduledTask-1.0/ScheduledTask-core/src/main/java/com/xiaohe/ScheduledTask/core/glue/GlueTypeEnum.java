package com.xiaohe.ScheduledTask.core.glue;

/**
 * @author : 小何
 * @Description : 定时任务的调度模式
 * 此处的调度模式不是调度中心选择哪个执行器去执行定时任务的模式，那个是负载策略。这个是执行器去执行的时候的策略(通常都是bean模式)
 * @date : 2023-08-25 00:14
 */
public enum GlueTypeEnum {
    //通常都是bean模式
    BEAN("BEAN", false, null, null),
    GLUE_GROOVY("GLUE(Java)", false, null, null),
    GLUE_SHELL("GLUE(Shell)", true, "bash", ".sh"),
    GLUE_PYTHON("GLUE(Python)", true, "python", ".py"),
    GLUE_PHP("GLUE(PHP)", true, "php", ".php"),
    GLUE_NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    GLUE_POWERSHELL("GLUE(PowerShell)", true, "powershell", ".ps1");

    private String desc;
    private boolean isScript;
    private String cmd;
    private String suffix;

    private GlueTypeEnum(String desc, boolean isScript, String cmd, String suffix) {
        this.desc = desc;
        this.isScript = isScript;
        this.cmd = cmd;
        this.suffix = suffix;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isScript() {
        return isScript;
    }

    public String getCmd() {
        return cmd;
    }

    public String getSuffix() {
        return suffix;
    }

    public static GlueTypeEnum match(String name){
        for (GlueTypeEnum item: GlueTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

}
