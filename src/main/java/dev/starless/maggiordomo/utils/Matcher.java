package dev.starless.maggiordomo.utils;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;
import java.util.regex.Pattern;

@UtilityClass
public class Matcher {

    // The USERNAME_TAG pattern is taken directly from JDA source code, credits to them
    // @ https://github.com/DV8FromTheWorld/JDA/blob/master/src/main/java/net/dv8tion/jda/api/entities/User.java

    private final Pattern USERNAME_TAG = Pattern.compile("(.{2,32})#(\\d{4})");
    private final Pattern ID = Pattern.compile("\\d{18,20}");
    private final Pattern HANDLE = Pattern.compile("^@?(\\w|\\.){2,32}$");

    public Optional<Member> getMemberFromInput(Guild guild, String input) {
        Member member = null;
        input = input.replaceAll(" ", "");

        if (isID(input)) {
            member = guild.getMemberById(input);
        } else if (isFullUsername(input)) {
            member = guild.getMemberByTag(input);
        } else if(isHandle(input)) {
            member = guild.getMemberByTag(input.replace("@", "").concat("#0000"));
        }

        return Optional.ofNullable(member);
    }

    private boolean isFullUsername(String string) {
        return USERNAME_TAG.matcher(string).matches();
    }

    private boolean isID(String string) {
        return ID.matcher(string).matches();
    }

    private boolean isHandle(String string) {
        return HANDLE.matcher(string).matches();
    }
}
