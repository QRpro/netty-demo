package neil.netty.runtime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;



public class Client {
	//静态内部类实现单例模式
	private static class SingletonHolder {
		static final Client instance = new Client();
	}
	
	public static Client getInstance(){
		return SingletonHolder.instance;
	}
	
	private EventLoopGroup group;
	private Bootstrap b;
	private ChannelFuture cf ;
	
	private Client(){
			group = new NioEventLoopGroup();
			b = new Bootstrap();
			b.group(group)
			 .channel(NioSocketChannel.class)
			 .handler(new LoggingHandler(LogLevel.INFO))
			 .handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel sc) throws Exception {
						sc.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingDecoder());
						sc.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingEncoder());
						//超时handler（当服务器端与客户端在指定时间以上没有任何进行通信，则会关闭响应的通道，主要为减小服务端资源占用）
						sc.pipeline().addLast(new ReadTimeoutHandler(5)); 
						sc.pipeline().addLast(new ClientHandler());
					}
		    });
	}
	
	public void connect(){
		try {
			this.cf = b.connect("127.0.0.1", 8765).sync();
			System.out.println("远程服务器已经连接, 可以进行数据交换..");				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//获取链接的方法
	public ChannelFuture getChannelFuture(){
		//如果连接不存在或者当前连接不是活动中的重新获取链接
		if(this.cf == null){
			this.connect();
		}
		if(!this.cf.channel().isActive()){
			this.connect();
		}
		
		return this.cf;
	}
	
	public static void main(String[] args) throws Exception{
		final Client c = Client.getInstance();
		
		ChannelFuture cf = c.getChannelFuture();
		for(int i = 1; i <= 3; i++ ){
			Request request = new Request();
			request.setId("" + i);
			request.setName("pro" + i);
			request.setRequestMessage("数据信息" + i);
			cf.channel().writeAndFlush(request);
			//休眠4秒，上面超时时间5秒故此处有数据发送不会超时，数据 循环三次传输完毕后链接超时
			TimeUnit.SECONDS.sleep(4);
		}

		cf.channel().closeFuture().sync();
		//模拟重新建立连接
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("进入子线程...");
					//重新获取ChannelFuture
					ChannelFuture cf = c.getChannelFuture();
					//再次发送数据
					Request request = new Request();
					request.setId("" + 4);
					request.setName("pro" + 4);
					request.setRequestMessage("数据信息" + 4);
					cf.channel().writeAndFlush(request);					
					cf.channel().closeFuture().sync();
					System.out.println("子线程结束.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		
		System.out.println("断开连接,主线程结束..");
	}
}
