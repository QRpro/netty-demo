package neil.netty.heartBeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

public class ClientHeartBeatHandler extends SimpleChannelInboundHandler<String> {
	//创建可执行定时任务的线程池，初始化核心线程数为1
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //Future模式，异步获取资源，表示ScheduledExecutorService返回的结果
    private ScheduledFuture<?> heartBeat;
	//主动向服务器发送认证信息
    private InetAddress addr ;
    //用于模拟对Server返回信息的验证
    private static final String SUCCESS_KEY = "auth_success_key";

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
		String key = "12345";
		//模拟证书，进行安全认证
		String auth = ip + "," + key;
		ctx.writeAndFlush(auth);
	}
	
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    	
        	if(msg instanceof String){
        		String ret = (String)msg;
        		if(SUCCESS_KEY.equals(ret)){
        	    	// 握手成功，主动发送心跳消息 参数意义：从第0秒开始每两秒执行 HeartBeatTask的任务
        	    	this.heartBeat = this.scheduler.scheduleWithFixedDelay(new HeartBeatTask(ctx), 0, 2, TimeUnit.SECONDS);
        		    System.out.println(msg);    			
        		}
        		else {
        			System.out.println(msg);
        		}
        	}
    }
    
    //具体任务
    private class HeartBeatTask implements Runnable {
    	private final ChannelHandlerContext ctx;

		public HeartBeatTask(final ChannelHandlerContext ctx) {
		    this.ctx = ctx;
		}
	
		@Override
		public void run() {
			try {
				//自定义的请求对象，用于封装计算机运行数据
			    ReqInfo info = new ReqInfo();
			    //ip
			    info.setIp(addr.getHostAddress());
		        Sigar sigar = new Sigar();
		        //cpu 
		        CpuPerc cpuPerc = sigar.getCpuPerc();
		        HashMap<String, Object> cpuPercMap = new HashMap<String, Object>();
		        cpuPercMap.put("combined", cpuPerc.getCombined());
		        cpuPercMap.put("user", cpuPerc.getUser());
		        cpuPercMap.put("sys", cpuPerc.getSys());
		        cpuPercMap.put("wait", cpuPerc.getWait());
		        cpuPercMap.put("idle", cpuPerc.getIdle());
		        // 内存
		        Mem mem = sigar.getMem();
				HashMap<String, Object> memoryMap = new HashMap<String, Object>();
				memoryMap.put("total", mem.getTotal() / 1024L);
				memoryMap.put("used", mem.getUsed() / 1024L);
				memoryMap.put("free", mem.getFree() / 1024L);
				info.setCpuPercMap(cpuPercMap);
			    info.setMemoryMap(memoryMap);
			    ctx.writeAndFlush(info);
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	cause.printStackTrace();
    	if (heartBeat != null) {
    		heartBeat.cancel(true);
    		heartBeat = null;
    	}
    	ctx.fireExceptionCaught(cause);
    }
}
