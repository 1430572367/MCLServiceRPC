package com.mcl.client;
 
import com.mcl.bean.RpcRequest;
import com.mcl.bean.RpcResponse;
import com.mcl.codec.RpcDecoder;
import com.mcl.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC真正调用客户端
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);
 
    private String host;
    private int port;
    private RpcResponse response;
    private final Object obj = new Object();
    // 使用 map 维护 id 和 Future 的映射关系，在多线程环境下需要使用线程安全的容器
    private final Map<String, DefaultFuture> futureMap = new ConcurrentHashMap<>();
 
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
 
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        /*synchronized (obj) {
            obj.notifyAll();   //收到响应，唤醒线程
        }*/
            // 获取数据的时候 将结果放入 future 中
            DefaultFuture defaultFuture = futureMap.get(response.getRequestId());
            defaultFuture.setResponse(response);

    }
 
    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                            .addLast(new RpcEncoder(RpcRequest.class)) // 将 RPC 请求进行编码（为了发送请求）
                            .addLast(new RpcDecoder(RpcResponse.class)) // 将 RPC 响应进行解码（为了处理响应）
                            .addLast(RpcClient.this); // 使用 RpcClient 发送 RPC 请求，这里把clientHandler的工作写到了client里头，没有分成client和clientHandler两个类
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);
 
            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().writeAndFlush(request).sync();
 
            /*synchronized (obj) {
                obj.wait();    //未收到响应，使线程等待
            }*/

            // 写数据的时候，增加映射
            futureMap.putIfAbsent(request.getRequestId(),new DefaultFuture());

            if (response != null) {
                future.channel().closeFuture().sync();
            }else{
                // 从 future 中获取真正的结果。
                DefaultFuture defaultFuture = futureMap.get(request.getRequestId());
                return defaultFuture.getResponse(10);
            }
            return response;
        } finally {
            // 完成后从 map 中移除。
            futureMap.remove(request.getRequestId());
            group.shutdownGracefully();
        }
    }

     @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}