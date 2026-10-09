package com.example.fakecf;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-only. Nothing is sent to the server.
 *
 * /fakecf win  <opponent> <amount>   e.g. /fakecf win SumitWasTaken90 36.1b
 * /fakecf lose <opponent> <amount>   e.g. /fakecf lose FMOFFICAL1M 11.4b
 */
public class FakeCfMod implements ClientModInitializer {

    // Your own name
    private static final String ME = "Momeratic1";

    // Colors sampled from your screenshots (shadow is vanilla: color / 4, automatic)
    private static final int YELLOW = 0xFCF744; // GAME SUMMARY
    private static final int GREEN  = 0x36F397; // win
    private static final int RED    = 0xFC1E53; // loss
    private static final int WHITE  = 0xFFFFFF; // labels

    // Measured from screenshots: title starts 45px in, result starts 60px in.
    // Bold space = 5px, so 9 and 12 bold spaces. WINNER and LOSER share the same offset.
    private static final String TITLE_PAD  = " ".repeat(9);
    private static final String RESULT_PAD = " ".repeat(12);
    private static final String BULLET = "\u2022 ";

    private static final Pattern AMT = Pattern.compile("^(\\d+(?:\\.\\d+)?)([kmbtKMBT]?)$");

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("fakecf")
                .then(ClientCommandManager.literal("win")
                    .then(ClientCommandManager.argument("opponent", StringArgumentType.word())
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                            .executes(ctx -> {
                                run(true,
                                    StringArgumentType.getString(ctx, "opponent"),
                                    StringArgumentType.getString(ctx, "amount"));
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("lose")
                    .then(ClientCommandManager.argument("opponent", StringArgumentType.word())
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                            .executes(ctx -> {
                                run(false,
                                    StringArgumentType.getString(ctx, "opponent"),
                                    StringArgumentType.getString(ctx, "amount"));
                                return 1;
                            }))))));
    }

    private static void run(boolean won, String opponent, String amountText) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long amount = parseAmount(amountText);
        if (amount < 0) {
            mc.player.sendMessage(Text.literal("Bad amount. Use e.g. 500m, 36.1b, 1.5t")
                .styled(st -> st.withColor(RED)), false);
            return;
        }
        String money = "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);

        int c = won ? GREEN : RED;
        String winner = won ? ME : opponent;
        String loser  = won ? opponent : ME;

        mc.execute(() -> {
            mc.player.sendMessage(Text.literal(""), false);
            mc.player.sendMessage(
                col(TITLE_PAD + "GAME SUMMARY", YELLOW, true), false);
            mc.player.sendMessage(
                col(RESULT_PAD + (won ? "WINNER" : "LOSER"), c, true), false);
            mc.player.sendMessage(Text.literal(""), false);
            mc.player.sendMessage(row(c, "Amount Bet: ", money), false);
            mc.player.sendMessage(row(c, "Winner: ", winner), false);
            mc.player.sendMessage(row(c, "Loser: ", loser), false);
            mc.player.sendMessage(Text.literal(""), false);
        });
    }

    // Bullet and value colored, label white
    private static MutableText row(int color, String label, String value) {
        return col(BULLET, color, false)
            .append(col(label, WHITE, false))
            .append(col(value, color, false));
    }

    private static MutableText col(String text, int rgb, boolean bold) {
        return Text.literal(text).styled(st -> st.withColor(rgb).withBold(bold));
    }

    private static long parseAmount(String s) {
        Matcher m = AMT.matcher(s);
        if (!m.matches()) return -1;
        double v = Double.parseDouble(m.group(1));
        switch (m.group(2).toLowerCase()) {
            case "k": v *= 1e3;  break;
            case "m": v *= 1e6;  break;
            case "b": v *= 1e9;  break;
            case "t": v *= 1e12; break;
        }
        return Math.round(v);
    }
}
