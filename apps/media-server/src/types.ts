import type { MIMEType } from "node:util";

export interface MediaFileMetadata {
  id: string;
  name: string;
  size: number;
  creationTimestamp: Date;
  lastUpdatedTimestamp: Date;
  mimeType: MIMEType;
  location?: { latitude: number; longitude: number };
}

export interface MediaFile extends MediaFileMetadata {
  content: Blob;
}

/**
 * One entry in the media-server's file listing (`GET /api/media`). A lightweight
 * summary for browsing: fetch the file itself from `url`, which is served by the
 * media-server's static route.
 */
export interface MediaFileSummary {
  /** File name, unique within the media directory. */
  name: string;
  /** Size in bytes. */
  size: number;
  /** Path to the file on the media-server, e.g. `/media/clip.mp4` (URL-encoded). */
  url: string;
}
