package config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import soundsystem.CDPlayer;
import soundsystem.CompactDisc;
import soundsystem.SgtPeppers;

//@Configuration表明这个类是一个配置1类，即不应该倾入到业务逻辑代码中，应该单独放在一个包中
@Configuration
/*
 1.basePackages:指定要扫描的基础包名称（一个或者多个），默认会扫描配置类所在的包
 2.basePackageClasses:指定一个或者多个类，Spring会去扫描这些类所在的包 ？ 可能会出现歧义？不同的包内
  会出现名称相同的类
 */
//@ComponentScan(basePackages = "soundsystem")
public class CDPlayerConfig {
    //显示的进行bean的创建,即这个方法不管怎么逻辑，最后返回一个实例，这个实例就是相对应的bean
    @Bean
    public CompactDisc sgtPeppers() {
        return new SgtPeppers();
    }

    @Bean
    public CDPlayer cdPlayer() {
        return new CDPlayer(sgtPeppers());  // 仅仅返回的是一个bean
    }

    //会自动进行组件扫描

}
