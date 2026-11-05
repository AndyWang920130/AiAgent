// The User Guide's table of contents.
//
// Tutorials are data, not components: GuideTutorialView renders any entry here, so adding a
// tutorial means adding an entry plus its locale keys — no new view and no router change.
// All prose lives in the locale files under `guide.`; an entry only says which keys exist.
import type { Component } from 'vue'
import { UserAddOutlined, CreditCardOutlined } from '@ant-design/icons-vue'

/** A button at the foot of a tutorial, linking to the feature it describes. */
export interface GuideLink {
  /** Locale key for the button label. */
  labelKey: string
  path: string
}

export interface GuideEntry {
  /** URL segment: /guide/<slug>. */
  slug: string
  /**
   * Locale key prefix under `guide.`. A tutorial with key 'follow' reads `guide.followTitle`,
   * `guide.followDesc`, `guide.followIntro` and `guide.followStep1Title` upwards.
   */
  key: string
  icon: Component
  /** Number of step<n>Title/step<n>Body pairs. */
  steps: number
  /** True when a `<key>Prereq` warning exists, shown above the steps. */
  prereq?: boolean
  /** True when a `<key>Tip` note exists, shown below them. */
  tip?: boolean
  links: GuideLink[]
}

export const guides: GuideEntry[] = [
  {
    slug: 'follow-users',
    key: 'follow',
    icon: UserAddOutlined,
    steps: 5,
    tip: true,
    links: [
      { labelKey: 'guide.openPersonal', path: '/personal' },
      { labelKey: 'guide.openHome', path: '/home' },
    ],
  },
  {
    slug: 'invite-and-play-70235',
    key: 'cardGame',
    icon: CreditCardOutlined,
    steps: 8,
    // You can only invite people who already follow you, which is worth saying before
    // someone opens the lobby and finds an empty invite list.
    prereq: true,
    tip: true,
    links: [
      { labelKey: 'guide.open70235', path: '/mini-game/card-games/70235' },
      { labelKey: 'guide.openNotifications', path: '/notifications' },
    ],
  },
]

export function findGuide(slug: string | string[] | undefined): GuideEntry | undefined {
  if (typeof slug !== 'string') return undefined
  return guides.find(g => g.slug === slug)
}
