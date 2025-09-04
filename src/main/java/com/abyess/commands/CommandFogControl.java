package com.abyess.commands;

import com.abyess.tracker.FogRenderer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandFogControl extends CommandBase {

    @Override
    public String getName() {
        return "fogcontrol";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/fogcontrol <off|config|manual>\n" +
                "  /fogcontrol manual exp2 <red_0-1> <green_0-1> <blue_0-1> <density_0-1>\n" +
                "  /fogcontrol manual linear <red_0-1> <green_0-1> <blue_0-1> <start_0-1> <end_0-1>"; // Updated usage for linear fog
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage:\n" + getUsage(sender)));
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "off":
                FogRenderer.setFogControlMode(FogRenderer.FogControlMode.DISABLED);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fog control mode set to DISABLED."));
                break;
            case "config":
                FogRenderer.setFogControlMode(FogRenderer.FogControlMode.CONFIG_BASED);
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fog control mode set to CONFIG_BASED."));
                break;
            case "manual":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage:\n" + getUsage(sender)));
                    return;
                }
                String subMode = args[1].toLowerCase();

                try {
                    float r, g, b;
                    if (args.length < 5) {
                        throw new CommandException("Not enough arguments for manual mode color. Usage:\n" + getUsage(sender));
                    }
                    r = Float.parseFloat(args[2]);
                    g = Float.parseFloat(args[3]);
                    b = Float.parseFloat(args[4]);

                    switch (subMode) {
                        case "exp2":
                            if (args.length < 6) {
                                throw new CommandException("Usage: /fogcontrol manual exp2 <red_0-1> <green_0-1> <blue_0-1> <density_0-1>");
                            }
                            float density = Float.parseFloat(args[5]);
                            FogRenderer.setManualFogExp2(r, g, b, density);
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Manual EXP2 fog set to RGB(" + String.format("%.2f", r) + "," + String.format("%.2f", g) + "," + String.format("%.2f", b) + "), Density=" + String.format("%.3f", density) + "."));
                            break;
                        case "linear":
                            if (args.length < 7) {
                                throw new CommandException("Usage: /fogcontrol manual linear <red_0-1> <green_0-1> <blue_0-1> <start_0-1> <end_0-1>"); // Updated usage message for linear
                            }
                            float start = Float.parseFloat(args[5]);
                            float end = Float.parseFloat(args[6]);
                            if (start < 0.0f || start > 1.0f || end < 0.0f || end > 1.0f) { // Validate percentage range
                                throw new CommandException("For LINEAR fog, start and end values must be between 0.0 and 1.0 (percentages).");
                            }
                            if (start >= end) {
                                throw new CommandException("For LINEAR fog, start percentage must be less than end percentage.");
                            }
                            FogRenderer.setManualFogLinear(r, g, b, start, end);
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Manual LINEAR fog set to RGB(" + String.format("%.2f", r) + "," + String.format("%.2f", g) + "," + String.format("%.2f", b) + "), Start=" + String.format("%.2f", start) + ", End=" + String.format("%.2f", end) + "."));
                            break;
                        default:
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown manual fog type: " + subMode + ". Use 'exp2' or 'linear'."));
                            return;
                    }
                } catch (NumberFormatException e) {
                    throw new CommandException("Invalid number format. Ensure colors, density, start, and end are valid floats. Error: " + e.getMessage());
                } catch (CommandException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CommandException("An unexpected error occurred: " + e.getMessage());
                }
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown mode: " + mode + ". Use 'off', 'config', or 'manual'."));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "off", "config", "manual");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("manual")) {
            return getListOfStringsMatchingLastWord(args, "exp2", "linear");
        }
        if ((args.length >= 3 && args.length <= 5) && args[0].equalsIgnoreCase("manual") &&
                (args[1].equalsIgnoreCase("exp2") || args[1].equalsIgnoreCase("linear"))) {
            return getListOfStringsMatchingLastWord(args, "0.0", "0.5", "1.0");
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("manual") && args[1].equalsIgnoreCase("exp2")) {
            return getListOfStringsMatchingLastWord(args, "0.0", "0.01", "0.05", "0.1", "0.2");
        }
        // Tab completion for start/end (linear) - args[5], args[6]
        // Now suggests percentages (0.0 to 1.0) instead of absolute distances
        if ((args.length == 6 || args.length == 7) && args[0].equalsIgnoreCase("manual") && args[1].equalsIgnoreCase("linear")) {
            return getListOfStringsMatchingLastWord(args, "0.0", "0.25", "0.5", "0.75", "1.0"); // Suggest common percentage values
        }

        return super.getTabCompletions(server, sender, args, targetPos);
    }
}