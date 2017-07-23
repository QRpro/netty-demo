package neil.netty.heartBeat;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;

public class ServerHeartBeatHandler extends ChannelInboundHandlerAdapter {
    
	private static HashMap<String, String> AUTH_IP_MAP = new HashMap<String, String>();
	private static final String SUCCESS_KEY = "auth_success_key";
	
	static {
		//初始化链接信息
		AUTH_IP_MAP.put("192.168.0.10", "12345");
	}
	
	private boolean auth(ChannelHandlerContext ctx, Object msg){
			//获取发送过来的证书进行校验
			String [] ret = ((String) msg).split(",");
			String auth = AUTH_IP_MAP.get(ret[0]);
			if(auth != null && auth.equals(ret[1])){
				ctx.writeAndFlush(SUCCESS_KEY);
				return true;
			} else {
				ctx.writeAndFlush("认证失败 !").addListener(ChannelFutureListener.CLOSE);
				return false;
			}
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof String){
			auth(ctx, msg);
		} else if (msg instanceof ReqInfo) {
			
			ReqInfo info = (ReqInfo) msg;
			System.out.println("--------------------------------------------");
			System.out.println("当前主机ip为: " + info.getIp());
			System.out.println("当前主机cpu情况: ");
			HashMap<String, Object> cpu = info.getCpuPercMap();
			System.out.println("总使用率: " + cpu.get("combined"));
			System.out.println("用户使用率: " + cpu.get("user"));
			System.out.println("系统使用率: " + cpu.get("sys"));
			System.out.println("等待率: " + cpu.get("wait"));
			System.out.println("空闲率: " + cpu.get("idle"));
			
			System.out.println("当前主机memory情况: ");
			HashMap<String, Object> memory = info.getMemoryMap();
			System.out.println("内存总量: " + memory.get("total"));
			System.out.println("当前内存使用量: " + memory.get("used"));
			System.out.println("当前内存剩余量: " + memory.get("free"));
			System.out.println("--------------------------------------------");
			
			ctx.writeAndFlush("信息已接收!");
		} else {
			ctx.writeAndFlush("链接失败!").addListener(ChannelFutureListener.CLOSE);
		}
    }


}
