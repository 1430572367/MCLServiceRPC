package com.mcl.server;
 //netty
import com.mcl.RpcService;
import com.mcl.bean.RpcRequest;
import com.mcl.bean.RpcResponse;
import com.mcl.codec.RpcDecoder;
import com.mcl.codec.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//spring框架
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
 
import java.util.HashMap;
import java.util.Map;
 
/**
* @Description:    服务端：缓存服务方法，建立连接
* @Author:         MaiChengLin
* @CreateDate:     2019/1/21 17:13
* @UpdateUser:     MaiChengLin
* @UpdateDate:     2019/1/21 17:13
* @UpdateRemark:   修改内容
* @Version:        1.0
*/
public class RpcServer implements ApplicationContextAware, InitializingBean {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);  //打印日志
 
    private String serverAddress;   //服务器地址
    private ServiceRegistry serviceRegistry;   //服务注册
    private Map<String, Object> handlerMap = new HashMap<>();   // 存放接口名与服务对象之间的映射关系
    
    //构造器1
    public RpcServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    //构造器2
    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }
 
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // 获取所有带有RpcService 注解的Spring Bean，例如ComService...
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) { //判断是否为空
            for (Object serviceBean : serviceBeanMap.values()) {
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            }
        }
    }

    //建立连接
    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RpcDecoder(RpcRequest.class))  // 将 RPC 请求进行解码（为了处理请求）
                                    .addLast(new RpcEncoder(RpcResponse.class)) // 将 RPC 响应进行编码（为了返回响应）
                                    .addLast(new RpcHandler(handlerMap));       // 处理 RPC 请求，把接口名与服务对象之间的映射关系传给handler
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            //注册到zookeeper
            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            ChannelFuture future = bootstrap.bind(host, port).sync();
            LOGGER.debug("server started on port {}", port);
            if (serviceRegistry != null) {
                serviceRegistry.register(serverAddress); // 注册服务地址
            }
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}