package dev.starless.maggiordomo.localization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Messages {

    // Strings related to the VC object
    VC_NAME("data.vc.default_name"),
    VC_OPEN_STATUS("data.vc.open_status"),
    VC_LOCKED_STATUS("data.vc.closed_status"),
    VC_BANNED("data.vc.banned"),
    VC_TRUSTED("data.vc.trusted"),

    // Strings used in the Settings object
    SETTINGS_INTERFACE_TITLE("data.settings.embed_title"),
    SETTINGS_INTERFACE_DESCRIPTION("data.settings.embed_description"),
    SETTINGS_CATEGORY_NAME("data.settings.category_name"),

    // Strings related to the filters (classes in the data.filter package)
    FILTER_BASIC("data.filters.basic_name"),
    FILTER_PATTERN("data.filters.pattern_name"),
    FILTER_FLAG_PREFIX("data.filters.flag_prefix"),
    FILTER_FLAG_CONTAINS("data.filters.flag_contains"),
    FILTER_FLAG_PATTERN("data.filters.flag_pattern"),
    FILTER_PATTERN_ERROR("data.filters.regex_error"),

    // Generic strings that are used throughout all the interactions
    NO_PERMISSION("interactions.no_permission"),
    NO_PERMISSION_BANNED("interactions.no_permission_banned"),
    COMMAND_NOT_FOUND("interactions.not_found"),
    GENERIC_ERROR("interactions.generic_error"),
    ON_COOLDOWN("interactions.on_cooldown"),

    // Translations of the commands
    COMMAND_BAN_ROLE_DESCRIPTION("commands.ban_role.description"),
    COMMAND_BAN_ROLE_PARAMETER_ROLE("commands.ban_role.parameters.role"),
    COMMAND_BAN_ROLE_SUCCESS_ADDED("commands.ban_role.success_added"),
    COMMAND_BAN_ROLE_SUCCESS_REMOVED("commands.ban_role.success_removed"),

    COMMAND_FILTERS_DESCRIPTION("commands.filters.description"),
    COMMAND_FILTERS_MESSAGE_CONTENT("commands.filters.message_content"),
    COMMAND_FILTERS_EXPLANATION("commands.filters.explanation"),
    COMMAND_FILTERS_ADD_BUTTON("commands.filters.add_button"),
    COMMAND_FILTERS_BASIC_INPUT("commands.filters.basic_input"),
    COMMAND_FILTERS_PATTERN_INPUT("commands.filters.pattern_input"),

    COMMAND_LANGUAGE_DESCRIPTION("commands.language.description"),
    COMMAND_LANGUAGE_PARAMETER_LANG("commands.language.parameters.language"),
    COMMAND_LANGUAGE_SUCCESS("commands.language.success"),
    COMMAND_LANGUAGE_FAIL("commands.language.unsupported"),

    COMMAND_MENU_DESCRIPTION("commands.menu.description"),
    COMMAND_MENU_SUCCESS("commands.menu.success"),
    COMMAND_MENU_FAIL("commands.menu.fail"),

    COMMAND_PREMIUM_ROLE_DESCRIPTION("commands.premium_role.description"),
    COMMAND_PREMIUM_ROLE_PARAMETERS_ROLE("commands.premium_role.parameters.role"),
    COMMAND_PREMIUM_ROLE_SUCCESS_ADDED("commands.premium_role.success_added"),
    COMMAND_PREMIUM_ROLE_SUCCESS_REMOVED("commands.premium_role.success_removed"),

    COMMAND_RECOVER_DESCRIPTION("commands.recover.description"),
    COMMAND_RECOVER_PARAMETER_CHANNEL("commands.recover.parameters.channel"),
    COMMAND_RECOVER_PARAMETER_PINNED("commands.recover.parameters.pinned"),
    COMMAND_RECOVER_MISSING_OWNER("commands.recover.missing_owner"),
    COMMAND_RECOVER_SUCCESS("commands.recover.success"),
    COMMAND_RECOVER_NOT_CORRUPTED("commands.recover.not_corrupted"),
    COMMAND_RECOVER_NOT_A_VC("commands.recover.not_a_voice_channel"),

    COMMAND_RELOAD_PERMS_DESCRIPTION("commands.reload_perms.description"),
    COMMAND_RELOAD_PERMS_WAITING("commands.reload_perms.waiting"),

    COMMAND_SETUP_DESCRIPTION("commands.setup.description"),
    COMMAND_SETUP_EXPLANATION("commands.setup.explanation"),
    COMMAND_SETUP_START_BUTTON_LABEL("commands.setup.start_button"),
    COMMAND_SETUP_CONTINUE_BUTTON_LABEL("commands.setup.continue_button"),
    COMMAND_SETUP_MENU_ERROR("commands.setup.cannot_create_menu"),
    COMMAND_SETUP_SUCCESS("commands.setup.success"),
    // First step: role
    COMMAND_SETUP_STEPS_ROLE_CONTENT("commands.setup.steps.role.content"),
    COMMAND_SETUP_STEPS_ROLE_SELECTOR_PLACEHOLDER("commands.setup.steps.role.role_selector_placeholder"),
    COMMAND_SETUP_STEPS_ROLE_UPDATED("commands.setup.steps.role.updated"),
    // Second step: interface
    COMMAND_SETUP_STEPS_INTERFACE_CONTENT("commands.setup.steps.interface.content"),
    COMMAND_SETUP_STEPS_INTERFACE_PREVIEW("commands.setup.steps.interface.preview_prefix"),
    COMMAND_SETUP_STEPS_INTERFACE_PREVIEW_BUTTON("commands.setup.steps.interface.preview_button"),
    COMMAND_SETUP_STEPS_INTERFACE_EDIT_BUTTON("commands.setup.steps.interface.edit_button"),
    COMMAND_SETUP_STEPS_INTERFACE_MODAL_TITLE("commands.setup.steps.interface.modal_title_label"),
    COMMAND_SETUP_STEPS_INTERFACE_MODAL_DESC("commands.setup.steps.interface.modal_desc_label"),
    COMMAND_SETUP_STEPS_INTERFACE_UPDATED("commands.setup.steps.interface.updated"),
    // Third step: inactivity
    COMMAND_SETUP_STEPS_INACTIVITY_CONTENT("commands.setup.steps.inactivity.content"),
    COMMAND_SETUP_STEPS_INACTIVITY_SELECTION_PLACEHOLDER("commands.setup.steps.inactivity.day_selection_placeholder"),
    COMMAND_SETUP_STEPS_INACTIVITY_SELECTION_DEFAULT("commands.setup.steps.inactivity.day_selection_default"),
    COMMAND_SETUP_STEPS_INACTIVITY_DAYS("commands.setup.steps.inactivity.days"),

    // Translations of the interactions
    INTERACTION_BAN_SELF_ERROR("interactions.ban.self_error"),
    INTERACTION_BAN_ALREADY_BANNED("interactions.ban.already_banned"),
    INTERACTION_BAN_TRUSTED_ERROR("interactions.ban.trusted_error"),
    INTERACTION_BAN_ADMIN_ERROR("interactions.ban.admin_error"),
    INTERACTION_BAN_SUCCESS("interactions.ban.success"),
    INTERACTION_BAN_NOTIFICATION_TITLE("interactions.ban.notification.title"),
    INTERACTION_BAN_NOTIFICATION_DESC("interactions.ban.notification.description"),

    INTERACTION_DELETE_SUCCESS("interactions.delete.success"),

    INTERACTION_KICK_ERROR_EMPTY("interactions.kick.error_empty"),
    INTERACTION_KICK_ERROR_NOT_FOUND("interactions.kick.error_not_found"),
    INTERACTION_KICK_MESSAGE_CONTENT("interactions.kick.message_content"),
    INTERACTION_KICK_SUCCESS("interactions.kick.success"),

    INTERACTION_LIST_SELECTION_CONTENT("interactions.list.selection_content"),
    INTERACTION_LIST_SELECTION_PLACEHOLDER("interactions.list.selection_placeholder"),
    INTERACTION_LIST_CONTENT("interactions.list.list_content"),
    INTERACTION_LIST_EMPTY("interactions.list.list_empty"),

    INTERACTION_PIN_PINNED("interactions.pin.pinned"),
    INTERACTION_PIN_UNPINNED("interactions.pin.unpinned"),

    INTERACTION_RESET_SUCCESS("interactions.reset.success"),

    INTERACTION_SIZE_FORMAT_ERROR("interactions.size.format_error"),
    INTERACTION_SIZE_SUCCESS("interactions.size.success"),

    INTERACTION_STATUS_CURRENT("interactions.status.current_status"),
    INTERACTION_SELECTION_TITLE("interactions.status.selection_title"),
    INTERACTION_SUCCESS_OPEN("interactions.status.success_open"),
    INTERACTION_SUCCESS_LOCKED("interactions.status.success_locked"),

    INTERACTION_TITLE_SUCCESS("interactions.title.success"),
    INTERACTION_TITLE_MODAL_TITLE("interactions.title.modal.title"),
    INTERACTION_TITLE_MODAL_INPUT_LABEL("interactions.title.modal.input_label"),
    INTERACTION_TITLE_MODAL_INPUT_PLACEHOLDER("interactions.title.modal.input_placeholder"),

    INTERACTION_TRUST_SELF_ERROR("interactions.trust.self_error"),
    INTERACTION_TRUST_ALREADY_TRUSTED("interactions.trust.already_trusted"),
    INTERACTION_TRUST_BANNED_ERROR("interactions.trust.banned_error"),
    INTERACTION_TRUST_TARGET_BANNED("interactions.trust.target_banned"),
    INTERACTION_TRUST_SUCCESS("interactions.trust.success"),
    INTERACTION_TRUST_NOTIFICATION_TITLE("interactions.trust.notification.title"),
    INTERACTION_TRUST_NOTIFICATION_DESC("interactions.trust.notification.description"),

    INTERACTION_UNBAN_EMPTY("interactions.unban.empty"),
    INTERACTION_UNBAN_SUCCESS("interactions.unban.success"),
    INTERACTION_UNBAN_NOTIFICATION_TITLE("interactions.unban.notification.title"),
    INTERACTION_UNBAN_NOTIFICATION_DESC("interactions.unban.notification.description"),

    INTERACTION_UNTRUST_EMPTY("interactions.untrust.empty"),
    INTERACTION_UNTRUST_SUCCESS("interactions.untrust.success"),

    // Other strings used throughout the project
    CONFIRMATION_VALUE("common.confirmation_value"),
    CONFIRMATION_MODAL_TITLE("common.confirmation_modal.title"),
    CONFIRMATION_MODAL_INPUT_LABEL("common.confirmation_modal.input_label"),
    CONFIRMATION_MODAL_INPUT_VALUE("common.confirmation_modal.input_value"),
    CONFIRMATION_MODAL_NOT_CONFIRMED("common.confirmation_modal.not_confirmed"),

    MEMBER_MODAL_TITLE("common.member_modal.title"),
    MEMBER_MODAL_INPUT_VALUE("common.member_modal.input_value"),
    MEMBER_MODAL_INPUT_ERROR("common.member_modal.input_error"),

    USER_SELECTION_MESSAGE_CONTENT("common.user_selection_menu.message_content"),
    USER_SELECTION_PLACEHOLDER("common.user_selection_menu.placeholder"),

    FILTER_MENU_TITLE("common.filters_menu.title"),
    FILTER_MENU_VALUE_BASIC("common.filters_menu.value_basic"),
    FILTER_MENU_VALUE_PATTERN("common.filters_menu.value_pattern"),

    PREV_BUTTON("common.prev_button"),
    NEXT_BUTTON("common.next_button"),
    INVALID_PUB_ROLE("common.invalid_public_role"),
    NO_PUBLIC_ROLE("common.no_public_role"),
    NO_SELECTION("common.error_not_selection"),
    MEMBER_NOT_FOUND("common.member_not_found");

    private final String path;
}
