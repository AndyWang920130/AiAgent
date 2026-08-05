<script lang="ts" setup>
// Presentational Gomoku board. Renders a 15x15 grid from a 2D cell array and emits the
// clicked coordinates; all game logic lives in the parent (local hotseat or online match).
type Cell = 0 | 1 | 2

const props = defineProps<{
  board: Cell[][]
  lastMove: { r: number; c: number } | null
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'place', r: number, c: number): void }>()

function isLast(r: number, c: number): boolean {
  return props.lastMove?.r === r && props.lastMove?.c === c
}

function onCell(r: number, c: number): void {
  if (props.disabled) return
  emit('place', r, c)
}
</script>

<template>
  <div class="board-wrap">
    <div class="board" :class="{ over: disabled }">
      <div v-for="(row, r) in board" :key="r" class="board-row">
        <div
          v-for="(cell, c) in row"
          :key="c"
          class="cell"
          :class="{ filled: cell !== 0 }"
          @click="onCell(r, c)"
        >
          <span v-if="cell === 1" class="stone black" :class="{ last: isLast(r, c) }" />
          <span v-else-if="cell === 2" class="stone white" :class="{ last: isLast(r, c) }" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-wrap {
  overflow-x: auto;
}
.board {
  display: inline-block;
  background: #e3b96b;
  padding: 6px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.board.over .cell {
  cursor: not-allowed;
}
.board-row {
  display: flex;
}
.cell {
  width: 30px;
  height: 30px;
  box-sizing: border-box;
  border: 1px solid #b58842;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
}
.cell.filled {
  cursor: default;
}
.stone {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: block;
  z-index: 1;
}
.stone.black {
  background: radial-gradient(circle at 30% 30%, #666, #000);
}
.stone.white {
  background: radial-gradient(circle at 30% 30%, #fff, #cfcfcf);
  border: 1px solid #bbb;
}
.stone.last {
  box-shadow: 0 0 0 2px #ff4d4f;
}
</style>
