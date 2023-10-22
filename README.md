# ScheduledTask
# 简介
高情商 ：分布式任务调度平台
低情商 ：照着xxl-job抄的。😁
遗憾的是，由于没学过thymleaf，并且在仿写的过程中没有注意国际化问题，所以虽然项目可以启动，可以使用Postman访问，但无法访问前端界面。。。😭
# 差别
## 1. 添加xxl-job-auto-register模块
![任务的自动注册](https://typorehwf.oss-cn-chengdu.aliyuncs.com/20231022124306.png)
实现 @XxlJobRegister 注解，可以在代码里硬编码注册任务，以后可以在web端看到它并可以修改corn表达式。
- 优点：不用代码写一遍还打开web端再注册一遍。
- 缺点：在web端修改了任务后，比如修改了corn表达式，代码里面可能是一天一次，数据库里面是一天两次。也就是不一致问题。没办法你在web端修改了没办法直接将运行的代码改掉。添加了数据库表xxl_job_info_history，记录任务修改日志。
## 2. 修改sql防止并发情况
XxlJobInfoMapper 中有这么一句sql:
```xml
	<update id="scheduleUpdate" parameterType="com.cqfy.xxl.job.admin.core.model.XxlJobInfo"  >
		UPDATE xxl_job_info
		SET
			trigger_last_time = #{triggerLastTime},
			trigger_next_time = #{triggerNextTime},
			trigger_status = #{triggerStatus}
		WHERE id = #{id}
	</update>
```
也就是任务一次调度之后将任务的 下次执行时间、上次执行时间、任务状态 这三个字段修改掉。现在假设这个任务12点执行，5分钟可以执行一次。（这里只是假设）
12点的时候这个任务成功调度，执行器去执行，在12:04的时候我在web端将这个任务关闭，也就是将 trigger_status 设置为0. 一分钟后这个任务执行成功，执行器将回调结果发送给回调中心。
由于 回调结果 中并没有 trigger_status 这个变量，所以在执行 scheduleUpdate 时会将 trigger_status 修改为1（因为调度之前就是1）.这就造成了并发问题。
解决方法：
1. 把 scheduleUpdate 中 trigger_status = #{triggerStatus} 删掉
2. 在回调结果中添加trigger_status返回给调度中心，并在执行器端执行完任务后确定这个字段的值。多此一举了。
