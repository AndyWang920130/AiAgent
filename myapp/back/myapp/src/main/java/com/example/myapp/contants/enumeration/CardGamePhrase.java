package com.example.myapp.contants.enumeration;

/**
 * The phrases a 70235 player can send to the table, in the order the picker offers them.
 *
 * <p>Stored and transmitted as the code, never as a sentence. The app is bilingual, so a phrase
 * kept as text would reach a Chinese reader in whatever language the sender's browser happened
 * to be in; as a code, each client renders it through its own locale file and the same tap reads
 * as {@code 好牌！} to one player and "Nice play!" to another.
 *
 * <p>An enum rather than a set of allowed strings. Binding a request straight to this type is
 * what makes "predefined" a property of the server: the picker offers eight options, but nothing
 * stops a crafted POST, and an unknown code has to fail at the boundary rather than be stored
 * and become the reader's problem to render.
 *
 * <p>What is absent matters as much as what is here. Nothing in this list can be aimed at a
 * person — there is no "hurry up", no "you're bad", nothing with a target — which is what keeps a
 * fixed list a safety property rather than a convenience. Adding one would spend that.
 */
public enum CardGamePhrase {
    HELLO,
    GOOD_LUCK,
    NICE_PLAY,
    WOW,
    UNLUCKY,
    OOPS,
    THANKS,
    GOOD_GAME
}
