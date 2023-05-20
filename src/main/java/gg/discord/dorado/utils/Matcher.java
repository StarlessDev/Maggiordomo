package gg.discord.dorado.utils;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;
import java.util.regex.Pattern;

@UtilityClass
public class Matcher {

    // The USERNAME_TAG pattern is taken directly from JDA source code, credits to them
    // @ https://github.com/DV8FromTheWorld/JDA/blob/master/src/main/java/net/dv8tion/jda/api/entities/User.java

    private static final Pattern USERNAME_TAG = Pattern.compile("(.{2,32})#(\\d{4})");
    private static final Pattern ID = Pattern.compile("\\d{18,20}");

    private boolean isFullUsername(String string) {
        return USERNAME_TAG.matcher(string).matches();
    }

    private boolean isID(String string) {
        return ID.matcher(string)
                .results()
                .findAny()
                .isPresent();
    }

    public Optional<Member> getMemberFromInput(Guild guild, String input) {
        Member member = null;

        if (Matcher.isFullUsername(input)) {
            member = guild.getMemberByTag(input);
        } else if (Matcher.isID(input)) {
            member = guild.getMemberById(input);
        }

        return Optional.ofNullable(member);
    }
}
