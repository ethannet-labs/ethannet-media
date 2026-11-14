/** An internal site link rendered with `next/link`, with its label resolved. */
export interface NavLink {
  /** Visible link text. */
  label: string;
  /** App route, e.g. `/about`. */
  href: string;
}

/** An external profile or contact link, opened in a new tab, with its label resolved. */
export interface SocialLink {
  /** Visible link text, e.g. `GitHub`. */
  label: string;
  /** Absolute URL (`https://…` or `mailto:…`). */
  href: string;
}
