package soundsystem;

import org.springframework.stereotype.Component;

//使用了@Component注解，表明这是一个组件类。且Spring要为这个类创建bean
@Component
public class SgtPeppers implements CompactDisc {
    private String title = "Sgt. Pepper's Lonely Hearts Club Band";
    private String artist = "The Beatles";

    public void play() {
        System.out.println("Playing " + title + " by " + artist);
    }

}
