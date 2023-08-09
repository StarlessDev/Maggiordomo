package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.MessageProvider;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ListInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.reply(new MessageCreateBuilder()
                        .setContent(MessageProvider.getMessage(Messages.INTERACTION_LIST_SELECTION_CONTENT, settings.getLanguage()))
                        .addComponents(ActionRow.of(StringSelectMenu.create(getName())
                                .setPlaceholder(MessageProvider.getMessage(Messages.INTERACTION_LIST_SELECTION_PLACEHOLDER, settings.getLanguage()))
                                .addOption(MessageProvider.getMessage(Messages.VC_BANNED, settings.getLanguage()), "BAN")
                                .addOption(MessageProvider.getMessage(Messages.VC_TRUSTED, settings.getLanguage()), "TRUST")
                                .build()))
                        .build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        if (!e.getValues().isEmpty()) {
            String label = e.getValues().get(0);
            int page;
            try {
                page = Integer.parseInt(id.split(":")[1]);
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                page = 0;
            }

            RecordType type;
            try {
                type = RecordType.valueOf(label); // Ottieni il tipo selezionato...
            } catch (IllegalArgumentException ex) {
                // ...se per qualche motivo ambiguo non esiste
                // mandiamo un messaggio di errore altrettanto ambiguo
                e.replyEmbeds(Embeds.errorEmbed(MessageProvider.getMessage(Messages.GENERIC_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            StringBuilder sb = new StringBuilder(MessageProvider.getMessage(Messages.INTERACTION_LIST_CONTENT, settings.getLanguage())).append("\n\n");
            AtomicInteger integer = new AtomicInteger(0); // Numero iniziale di record

            // Filtra i record e forma il messaggio
            Set<UserRecord> records = vc.getTotalRecords();
            int elementsNumber = records.size();
            long skippedElements = 25L * page;
            records.stream()
                    .filter(record -> record.type().equals(type)) // Limitati solo ai record del tipo corretto
                    .skip(skippedElements) // Skippa alcuni elementi delle pagine precedenti
                    .limit(25) // Limita gli elementi a 25
                    .forEach(record -> {
                        integer.incrementAndGet();

                        Member member = e.getGuild().getMemberById(record.user());
                        if (member != null) {
                            sb.append("• ")
                                    .append(member.getAsMention())
                                    .append(String.format(" *(%s#%s)* ", member.getUser().getName(), member.getUser().getDiscriminator()))
                                    .append("\n");
                        }
                    });

            MessageCreateBuilder builder = new MessageCreateBuilder();
            int streamCount = integer.get();
            if (streamCount == 0) {
                // Messaggio nel caso che non ci sia nessuno
                sb.append(MessageProvider.getMessage(Messages.INTERACTION_LIST_EMPTY, settings.getLanguage())).append(type.equals(RecordType.BAN) ? ":rainbow: " : ":cry:");
            } else {
                sb.setLength(sb.length() - 2);

                List<Button> buttons = new ArrayList<>(2);
                // Se non è la prima pagina
                if (page != 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY,
                            getName() + ":" + (page - 1),
                            Emoji.fromUnicode("⏪")));
                }

                // Se non sono stati mostrati tutti gli elementi
                if (skippedElements + streamCount != elementsNumber) {
                    buttons.add(Button.of(ButtonStyle.PRIMARY,
                            getName() + ":" + (page + 1),
                            Emoji.fromUnicode("⏩")));
                }

                if (!buttons.isEmpty())
                    builder.addComponents(ActionRow.of(buttons)); // Aggiungi i bottoni al messaggio
            }

            // Manda il tutto
            e.reply(builder.setContent(sb.toString()).build())
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("📜");
    }

    @Override
    public String getName() {
        return "kick";
    }
}
