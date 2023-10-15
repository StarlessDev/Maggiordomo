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
    NEED_VC("interactions.need_vc"),
    GENERIC_ERROR("interactions.generic_error"),
    ON_COOLDOWN("interactions.on_cooldown"),

    // Translations of the commands
    COMMAND_FILTERS_DESCRIPTION("commands.filters.description"),
    COMMAND_FILTERS_MESSAGE_CONTENT("commands.filters.message_content"),
    COMMAND_FILTERS_EXPLANATION("commands.filters.explanation"),
    COMMAND_FILTERS_ADD_BUTTON("commands.filters.add_button"),
    COMMAND_FILTERS_BASIC_INPUT("commands.filters.basic_input"),
    COMMAND_FILTERS_PATTERN_INPUT("commands.filters.pattern_input"),
    COMMAND_FILTERS_UPDATED("commands.filters.updated"),

    COMMAND_LANGUAGE_DESCRIPTION("commands.language.description"),
    COMMAND_LANGUAGE_PARAMETER_LANG("commands.language.parameters.language"),
    COMMAND_LANGUAGE_SUCCESS("commands.language.success"),
    COMMAND_LANGUAGE_FAIL("commands.language.unsupported"),

    // Management command
    COMMAND_MANAGEMENT_DESCRIPTION("commands.management.description"),
    COMMAND_MANAGEMENT_MENU_CONTENT("commands.management.menu.content"),
    COMMAND_MANAGEMENT_MENU_PREMIUM_ROLES_BUTTON("commands.management.menu.buttons.premium_roles"),
    COMMAND_MANAGEMENT_MENU_BANNED_ROLES_BUTTON("commands.management.menu.buttons.banned_roles"),
    COMMAND_MANAGEMENT_MENU_FILTERS_BUTTON("commands.management.menu.buttons.filters"),
    COMMAND_MANAGEMENT_MENU_REFRESH_PERMS_BUTTON("commands.management.menu.buttons.refresh_perms"),
    COMMAND_MANAGEMENT_MENU_MANAGE_ROOMS_BUTTON("commands.management.menu.buttons.room_manager"),

    // Sections of the management command
    COMMAND_MANAGEMENT_LISTS_ADD_SELECTION_PLACEHOLDER("commands.management.sections.lists.add_selection_placeholder"),
    COMMAND_MANAGEMENT_LISTS_ADD_BUTTON_LABEL("commands.management.sections.lists.add_button_label"),
    COMMAND_MANAGEMENT_LISTS_REMOVE_PLACEHOLDER("commands.management.sections.lists.remove_selection_placeholder"),
    COMMAND_MANAGEMENT_LISTS_REMOVE_BUTTON_LABEL("commands.management.sections.lists.remove_button_label"),
    COMMAND_MANAGEMENT_LISTS_ROLE_ADDED("commands.management.sections.lists.role_added"),
    COMMAND_MANAGEMENT_LISTS_ROLE_REMOVED("commands.management.sections.lists.role_removed"),
    COMMAND_MANAGEMENT_LISTS_USER_REMOVED("commands.management.sections.lists.user_removed"),
    COMMAND_MANAGEMENT_LISTS_NO_ROLES("commands.management.sections.lists.no_roles"),
    COMMAND_MANAGEMENT_LISTS_ROLES_LIST("commands.management.sections.lists.roles_list"),

    COMMAND_MANAGEMENT_ROOMS_MAIN_MENU("commands.management.sections.rooms_manager.main_content"),
    COMMAND_MANAGEMENT_ROOMS_INSPECTION_MENU("commands.management.sections.rooms_manager.inspection_content"),
    COMMAND_MANAGEMENT_ROOMS_BUTTONS_PIN_LABEL("commands.management.sections.rooms_manager.buttons.pin_label"),
    COMMAND_MANAGEMENT_ROOMS_BUTTONS_UNPIN_LABEL("commands.management.sections.rooms_manager.buttons.unpin_label"),
    COMMAND_MANAGEMENT_ROOMS_BUTTONS_TITLE_LABEL("commands.management.sections.rooms_manager.buttons.title_label"),
    COMMAND_MANAGEMENT_ROOMS_BUTTONS_DELETE_LABEL("commands.management.sections.rooms_manager.buttons.delete_label"),
    COMMAND_MANAGEMENT_ROOMS_DROPDOWNS_TRUSTED_PLACEHOLDER("commands.management.sections.rooms_manager.placeholders.trusted"),
    COMMAND_MANAGEMENT_ROOMS_DROPDOWNS_BANNED_PLACEHOLDER("commands.management.sections.rooms_manager.placeholders.banned"),
    COMMAND_MANAGEMENT_ROOMS_DEFAULT_NICKNAME("commands.management.default_nickname"),
    COMMAND_MANAGEMENT_ROOMS_DEFAULT_USERNAME("commands.management.default_username"),
    COMMAND_MANAGEMENT_ROOMS_OWNED_BY("commands.management.sections.rooms_manager.owned_by"),
    COMMAND_MANAGEMENT_ROOMS_AKA("commands.management.sections.rooms_manager.aka"),
    COMMAND_MANAGEMENT_ROOMS_NOT_AVAILABLE("commands.management.sections.rooms_manager.not_available"),
    COMMAND_MANAGEMENT_ROOMS_FEEDBACK_PIN("commands.management.sections.rooms_manager.feedback.pinned"),
    COMMAND_MANAGEMENT_ROOMS_FEEDBACK_UNPIN("commands.management.sections.rooms_manager.feedback.temporary"),
    COMMAND_MANAGEMENT_ROOMS_FEEDBACK_DELETE("commands.management.sections.rooms_manager.feedback.delete"),

    COMMAND_MENU_DESCRIPTION("commands.menu.description"),
    COMMAND_MENU_SUCCESS("commands.menu.success"),
    COMMAND_MENU_FAIL("commands.menu.fail"),

    COMMAND_RECOVER_DESCRIPTION("commands.recover.description"),
    COMMAND_RECOVER_PARAMETER_CHANNEL("commands.recover.parameters.channel"),
    COMMAND_RECOVER_PARAMETER_PINNED("commands.recover.parameters.pinned"),
    COMMAND_RECOVER_MISSING_OWNER("commands.recover.missing_owner"),
    COMMAND_RECOVER_SUCCESS("commands.recover.success"),
    COMMAND_RECOVER_NOT_CORRUPTED("commands.recover.not_corrupted"),
    COMMAND_RECOVER_NOT_A_VC("commands.recover.not_a_voice_channel"),

    COMMAND_RELOAD_PERMS_WAITING("commands.reload_perms.waiting"),

    COMMAND_SETUP_DESCRIPTION("commands.setup.description"),
    COMMAND_SETUP_EXPLANATION("commands.setup.explanation"),
    COMMAND_SETUP_START_BUTTON_LABEL("commands.setup.start_button"),
    COMMAND_SETUP_CONTINUE_BUTTON_LABEL("commands.setup.continue_button"),
    COMMAND_SETUP_MENU_ERROR("commands.setup.cannot_create_menu"),
    COMMAND_SETUP_SUCCESS("commands.setup.success"),
    // First step: role
    COMMAND_SETUP_STEPS_ROLE_CONTENT("commands.setup.steps.role.content"),
    COMMAND_SETUP_STEPS_ROLE_RESET("commands.setup.steps.role.reset_button"),
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
    INTERACTION_STATUS_SELECTION_TITLE("interactions.status.selection_title"),
    INTERACTION_STATUS_SUCCESS_OPEN("interactions.status.success_open"),
    INTERACTION_STATUS_SUCCESS_LOCKED("interactions.status.success_locked"),

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
    SHORT_PREV_BUTTON("common.prev_button_short"),
    NEXT_BUTTON("common.next_button"),
    INVALID_PUB_ROLE("common.invalid_public_role"),
    NO_PUBLIC_ROLE("common.no_public_role"),
    NO_SELECTION("common.error_not_selection"),
    MEMBER_NOT_FOUND("common.member_not_found");

    private final String path;
}
