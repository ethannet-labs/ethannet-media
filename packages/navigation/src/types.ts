import type { UiMessages } from "@ethannet/messages";

/** Key of a site nav link's label in {@link UiMessages}`["Navigation"]`. */
export type NavLinkId = keyof UiMessages["Navigation"];

/** Key of a social link's label in {@link UiMessages}`["Social"]`. */
export type SocialLinkId = keyof UiMessages["Social"];

/**
 * A link as configured in code: where it goes, and which message key holds its
 * label. The label itself is copy, so it lives in the messages, not here.
 */
export interface LinkConfig<Id extends string> {
  /** Key of the link's label in the matching messages namespace. */
  id: Id;
  /** App route (`/about`) or absolute URL (`https://…`, `mailto:…`). */
  href: string;
}
