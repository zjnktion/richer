package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.IService;
import cn.zjnktion.richer.core.JDBCUtil;
import cn.zjnktion.richer.core.RedisUtil;
import cn.zjnktion.richer.core.protobuf.BusinessMsg;
import cn.zjnktion.richer.core.protobuf.RequestBodyMsg;
import com.google.protobuf.ExtensionRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

/**
 * @author zjnktion
 */
public class ConnectorService implements IService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorService.class);

    private static final int BOSS_EVENT_LOOPS = 1;
    private static final int WORKER_EVENT_LOOPS = 2 * Runtime.getRuntime().availableProcessors();

    private int bindPort = 8081;

    public ConnectorService() {

    }

    public ConnectorService(int bindPort) {
        this.bindPort = bindPort;
    }

    public void startup() {
        // 启动数据库连接服务
        JDBCUtil.DBConfiguration configuration = JDBCUtil.DBConfiguration.Builder.newBuilder()
                .setDriverClassName("com.mysql.jdbc.Driver")
                .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/richer?rewriteBatchedStatements=true")
                .setUsername("root")
                .setPassword("dg34672")
                .build();
        try {
            JdbcTemplate jdbcTemplate = JDBCUtil.generateMysqlTemplate(configuration);
            JDBC.initTemplate(jdbcTemplate);
        }
        catch (SQLException e) {
            LOGGER.error("Connector dataSource init occur exception:", e);
            throw new IllegalStateException("dataSource init failed");
        }

        // 启动缓存连接服务
        RedisUtil.Configuration redisConf = RedisUtil.Configuration.Builder.newBuilder()
                .setHostName("127.0.0.1")
                .build();
        RedisUtil.CompositeTemplate compositeTemplate = RedisUtil.generateRedisTemplate(redisConf);
        Redis.initTemplate(compositeTemplate);

        // 启动所有后台进程
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Thread[" + t.getName() + "] occur exception :", e);
            }
        });

        // 启动监听服务
        EventLoopGroup bossGroup = new NioEventLoopGroup(BOSS_EVENT_LOOPS);
        EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_EVENT_LOOPS);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 注册protobuf extensions
                            ExtensionRegistry registry = ExtensionRegistry.newInstance();
                            BusinessMsg.registerAllExtensions(registry);
                            RequestBodyMsg.registerAllExtensions(registry);

                            // decoder
                            ProtobufDecoder protobufDecoder = new ProtobufDecoder(BusinessMsg.Request.getDefaultInstance(), registry);
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                            ch.pipeline().addLast(protobufDecoder);

                            // encoder
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new ProtobufEncoder());

                            // business handler
                            ch.pipeline().addLast(new ConnectorHandler());
                        }
                    });

            // 绑定监听端口
            ChannelFuture channelFuture = bootstrap.bind(bindPort).sync();
            LOGGER.info("Connector start up and listen at port#{}...", bindPort);
            channelFuture.channel().closeFuture().sync();
        }
        catch (InterruptedException e) {
            LOGGER.error("Connector occur exception while starting up:", e);
            throw new IllegalStateException("Connector bind failed.");
        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
