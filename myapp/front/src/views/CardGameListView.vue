<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { CreditCardOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()
const router = useRouter()

interface CardGame {
  key: string
  /** The game's own name. Not translated — "70235" reads the same in every locale. */
  label: string
  desc: string
  /** Route name to open, or null while the game itself is still being built. */
  routeName: string | null
}

const games = computed<CardGame[]>(() => [
  {
    key: '70235',
    label: '70235',
    desc: t('cardGame.desc70235'),
    routeName: 'card-game-70235',
  },
])

function openGame(game: CardGame) {
  if (!game.routeName) return
  router.push({ name: game.routeName })
}
</script>

<template>
  <div class="card-game-list-view">
    <a-page-header
      :title="t('cardGame.pageTitle')"
      :sub-title="t('cardGame.pageSubtitle')"
    />

    <a-row :gutter="[16, 16]">
      <a-col v-for="game in games" :key="game.key" :xs="24" :sm="12" :md="8" :lg="6">
        <a-card
          :hoverable="!!game.routeName"
          class="game-card"
          :class="{ 'game-card-disabled': !game.routeName }"
          @click="openGame(game)"
        >
          <template #cover>
            <div class="game-cover">
              <CreditCardOutlined class="game-icon" />
            </div>
          </template>
          <a-card-meta :title="game.label" :description="game.desc" />
          <template #actions>
            <a-tag v-if="!game.routeName">{{ t('cardGame.comingSoon') }}</a-tag>
            <a-button v-else type="link">{{ t('cardGame.play') }}</a-button>
          </template>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.card-game-list-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.game-card {
  height: 100%;
}
.game-card:not(.game-card-disabled) {
  cursor: pointer;
}
.game-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  background: #f5f5f5;
}
.game-icon {
  font-size: 36px;
  color: #1890ff;
}
</style>
