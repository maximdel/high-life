package com.drunkgamblers.highlife.effect;

import com.drunkgamblers.highlife.registry.ModEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Changes what you type in chat depending on what you're on. Strongest effect wins. */
public final class ChatEffects {
    private static final String[] LAUGHS = {" hehehe", " HAHAHAHA", " *wheeze*", " hahah wait", " hihihi"};
    private static final String[] STONED = {" ...dude", " ...wait what was i saying", " lol", " ...anyone got snacks?", " haha yeah"};
    private static final String[] LOVE = {" <3", " i love you guys", " ur all so amazing", " group hug??"};
    private static final String[] TRIP = {" (the colors man)", " ...is the grass breathing?", " whoa", " (everything is connected)"};

    private static boolean has(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return e.hasEffect(effect);
    }

    private static String pick(String[] options, RandomSource r) {
        return options[r.nextInt(options.length)];
    }

    /** Returns the transformed message, or the original if nothing applies. */
    public static String transform(LivingEntity e, String msg) {
        RandomSource r = e.getRandom();
        if (has(e, ModEffects.BREAKTHROUGH)) return "i can see the machine elves... " + msg.toLowerCase();
        if (has(e, ModEffects.K_HOLE)) return stretch(msg.toLowerCase());
        if (has(e, ModEffects.SALVIA)) return shuffleWords(msg, r);
        if (has(e, ModEffects.GIGGLES)) return msg + pick(LAUGHS, r);
        if (has(e, ModEffects.SPEEDING) || has(e, ModEffects.WIRED)) return msg.toUpperCase() + "!!!";
        if (has(e, ModEffects.ROLLING)) return msg + pick(LOVE, r);
        if (has(e, ModEffects.ACID) || has(e, ModEffects.TRIPPING)) return msg + pick(TRIP, r);
        MobEffectInstance high = e.getEffect(ModEffects.HIGH);
        if (high != null && high.getAmplifier() >= 1 && r.nextInt(3) != 0) return msg + pick(STONED, r);
        return msg;
    }

    private static String stretch(String msg) {
        StringBuilder b = new StringBuilder();
        for (char c : msg.toCharArray()) {
            b.append(c);
            if ("aeiou".indexOf(c) >= 0) b.append(c).append(c);
        }
        return b + "...";
    }

    private static String shuffleWords(String msg, RandomSource r) {
        List<String> words = new ArrayList<>(Arrays.asList(msg.split(" ")));
        if (words.size() < 2) return new StringBuilder(msg).reverse().toString();
        Collections.shuffle(words, new java.util.Random(r.nextLong()));
        return String.join(" ", words);
    }

    private ChatEffects() {}
}
