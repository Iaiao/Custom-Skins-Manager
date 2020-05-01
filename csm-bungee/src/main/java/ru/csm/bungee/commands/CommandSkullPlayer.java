package ru.csm.bungee.commands;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.csm.api.network.Channels;
import ru.csm.api.network.MessageSender;
import ru.csm.api.player.Head;
import ru.csm.api.services.SkinsAPI;
import ru.csm.bungee.command.SubCommand;

public class CommandSkullPlayer extends SubCommand {

    private final String usage;

    private final SkinsAPI<ProxiedPlayer> api;
    private final MessageSender<ProxiedPlayer> sender;

    public CommandSkullPlayer(SkinsAPI<ProxiedPlayer> api, MessageSender<ProxiedPlayer> sender){
        this.api = api;
        this.sender = sender;
        this.usage = String.format(api.getLang().of("command.usage"), "/skull player <player>");
    }

    @Override
    public void exec(CommandSender sender, String[] args) {
        if (args.length == 1){
            if (sender instanceof ProxiedPlayer){
                ProxiedPlayer player = (ProxiedPlayer) sender;
                Head head = api.getPlayerHead(args[0]);

                if (head != null){
                    JsonObject message = new JsonObject();
                    message.addProperty("player", player.getName());
                    message.addProperty("url", head.getUrl());
                    this.sender.sendMessage(player, Channels.SKULLS, message);
                    return;
                }

                sender.sendMessage(String.format(api.getLang().of("player.missing"), args[0]));
            }

            return;
        }

        sender.sendMessage(usage);
    }
}