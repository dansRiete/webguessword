package aleks.kuzko.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Created by Aleks on 14.05.2017.
 */

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "aleks.kuzko")
public class Configuration2 {


}
