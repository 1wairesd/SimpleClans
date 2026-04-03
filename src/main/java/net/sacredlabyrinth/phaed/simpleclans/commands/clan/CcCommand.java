package net.sacredlabyrinth.phaed.simpleclans.commands.clan;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.managers.ChatManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.StorageManager;

import static net.sacredlabyrinth.phaed.simpleclans.ClanPlayer.Channel.CLAN;
import static net.sacredlabyrinth.phaed.simpleclans.ClanPlayer.Channel.NONE;
import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source.SPIGOT;

@CommandAlias("cc")
@Conditions("%basic_conditions|clan_member|can_chat:type=CLAN")
@CommandPermission("simpleclans.member.chat")
@Description("{@@command.description.cc}")
public class CcCommand extends BaseCommand {

    @Dependency
    private ChatManager chatManager;
    @Dependency
    private StorageManager storageManager;

    @Default
    @HelpSearchTags("chat")
    public void execute(ClanPlayer cp, @Optional @Name("message") String message) {
        if (message == null || message.isEmpty()) {
            toggleChat(cp);
        } else {
            chatManager.processChat(SPIGOT, CLAN, cp, message);
        }
    }

    private void toggleChat(ClanPlayer clanPlayer) {
        if (clanPlayer.getChannel() == CLAN) {
            clanPlayer.setChannel(NONE);
            storageManager.updateClanPlayer(clanPlayer);
            ChatBlock.sendMessage(clanPlayer, lang("left.clan.chat", clanPlayer));
        } else {
            clanPlayer.setChannel(CLAN);
            storageManager.updateClanPlayer(clanPlayer);
            ChatBlock.sendMessage(clanPlayer, lang("joined.clan.chat"));
        }
    }
}
