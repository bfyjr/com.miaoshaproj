package org.example.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

//当spring容器中没有TomcatEmbeddedServletContainerFactory这个bean时，会把此bean加载进来
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        //factory会在Spring容器初始化的时候传入
        //我们可以使用这个工厂类定制化开发tomcat connector
        TomcatServletWebServerFactory factory1 = (TomcatServletWebServerFactory) factory;
        factory1.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
                //定制化keepalive timeout,设置30秒内没有请求则断开连接
                protocolHandler.setKeepAliveTimeout(30000);
                //当客户端发送超过10000个请求，则自动断开连接
                protocolHandler.setMaxKeepAliveRequests(10000);
            }
        });


    }
}
