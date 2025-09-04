//package com.abyess.expansion;
//
//import net.minecraft.command.CommandBase;
//import net.minecraft.command.CommandException;
//import net.minecraft.command.ICommandSender;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.text.TextComponentString;
//import net.minecraft.util.text.TextFormatting; // Ensure TextFormatting is imported
//
//import com.abyess.expansion.CustomPlayerIconsManager;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class CommandSetPlayerIcon extends CommandBase {
//
//    @Override
//    public String getName() {
//        return "setplayericon";
//    }
//
//    @Override
//    public String getUsage(ICommandSender sender) {
//        return "/setplayericon <icon_id|0>";
//    }
//
//    @Override
//    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
//        // Ensure only players can execute this command, or adjust permission level as needed
//        return sender.getCommandSenderEntity() != null;
//    }
//
//    @Override
//    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
//        if (args.length == 0) {
//            // If no arguments, prompt for usage
//            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
//            return;
//        }
//
//        String iconId = args[0].toLowerCase();
//
//        if (iconId.equals("0")) {
//            // Player chose default icon (Steve or player skin)
//            CustomPlayerIconsManager.setPlayerSelectedIcon("0");
//         //   sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Player icon has been set to default."));
//        } else {
//            // Attempt to set a custom icon
//            if (CustomPlayerIconsManager.hasCustomIcon(iconId)) {
//                CustomPlayerIconsManager.setPlayerSelectedIcon(iconId);
//             //   sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Player icon has been set to '" + iconId + "'."));
//            } else {
//                // MODIFIED: If custom icon does not exist, provide a single, clear warning.
//                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Custom icon '" + iconId + "' not found. Make sure it's a 14x14 PNG in the 'config/curseofabyss/player_icons/' folder."));
//            }
//        }
//    }
//
//    @Override
//    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
//        if (args.length == 1) {
//            // Autocomplete "0" and all loaded custom icon names
//            Stream<String> iconNames = Stream.concat(
//                    Stream.of("0"), // Add default option "0"
//                    CustomPlayerIconsManager.getAllCustomIconNames().stream() // Get all custom icon names
//            );
//            return getListOfStringsMatchingLastWord(args, iconNames.collect(Collectors.toList()));
//        }
//        return Collections.emptyList();
//    }
//
//    @Override
//    public int getRequiredPermissionLevel() {
//        // Set permission level to 0, meaning all players can execute this command
//        // If you need to restrict permissions, you can set it to 1 (operators) or higher
//        return 0;
//    }
//}