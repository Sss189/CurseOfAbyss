// File: src/main/java/com/abyess/commands/CommandCurse.java
package com.abyess.commands;

import com.abyess.config.ModConfig; // Ensure this import path is correct
import com.abyess.tracker.CurseApplication;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCurse extends CommandBase {

    @Override
    public String getName() {
        return "applycurse";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TextFormatting.RED + "Usage: /applycurse <player> <type> <name> [duration_seconds] [amplifier] [boolean_value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) {
            throw new CommandException(getUsage(sender));
        }

        EntityPlayer targetPlayer = getPlayer(server, sender, args[0]);
        String curseType = args[1].toLowerCase();
        String curseName = args[2];

        int duration = 60; // Default duration 60 seconds
        int amplifier = 0; // Default amplifier 0
        boolean booleanValue = false; // Default boolean value false

        // Parse optional arguments
        if (args.length >= 4) {
            duration = parseInt(args[3], 1);
        }
        if (args.length >= 5) {
            amplifier = parseInt(args[4], 0);
        }
        if (args.length >= 6) {
            booleanValue = parseBoolean(args[5]);
        }

        // Create a temporary ModConfig.CurseConfig object to pass parameters
        ModConfig.CurseConfig tempCurseConfig = new ModConfig.CurseConfig(
                curseType,
                curseName,
                duration,
                amplifier,
                booleanValue
        );

        try {
            CurseApplication.applyCurse(targetPlayer, tempCurseConfig);
            // notifyCommandListener(sender, this, TextFormatting.GREEN + "Successfully applied curse '%s' of type '%s' to %s.", curseName, curseType, targetPlayer.getName()); // 移除成功反馈
        } catch (IllegalArgumentException e) {
            throw new CommandException(TextFormatting.RED + "Error applying curse: %s", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandException(TextFormatting.RED + "An unexpected error occurred while applying the curse.");
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) { // Player names
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else if (args.length == 2) { // Curse types
            return getListOfStringsMatchingLastWord(args, "potion", "filter", "other", "super_secret_setting");
        } else if (args.length == 3) { // Curse names (based on type)
            String curseType = args[1].toLowerCase();
            switch (curseType) {
                case "potion":
                    // Suggest some common vanilla potions, or all if feasible
                    return getListOfStringsMatchingLastWord(args,
                            Arrays.stream(Potion.REGISTRY.getKeys().toArray(new String[0]))
                                    .map(s -> s.replace("minecraft:", ""))
                                    .collect(Collectors.toList())
                    );
                case "filter":
                    // Hardcoded list from your FilterRegistry
                    return getListOfStringsMatchingLastWord(args, "inverted", "overlappingblur", "red");
                case "other":
                    // Hardcoded list from your applyOtherCurse
                    return getListOfStringsMatchingLastWord(args, "hollow", "confusion", "bleeding", "2dentities");
                case "super_secret_setting":
                    // Example shader IDs. If CustomShaderLoader has a method to list them, use that.
                    return getListOfStringsMatchingLastWord(args, "notch", "bits", "flip", "bumpy", "antialias", "art", "blobs", "blobs2", "blur", "color_convolve", "creeper", "deconverge", "desaturate", "fxaa", "green", "invert", "ntsc", "outline", "pencil", "phosphor", "scan_pincushion", "sobel", "spider", "wobble");
                default:
                    return Collections.emptyList();
            }
        } else if (args.length == 6) { // Boolean value (true/false)
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return completions;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName()); // Op level 2 (gamemaster)
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("curse", "acurse"); // Optional aliases
    }
}