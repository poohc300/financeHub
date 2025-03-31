<script setup lang="ts">
import { PropType, computed, ref, watch } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import { EventClickArg, CalendarOptions } from '@fullcalendar/core';
import { CalendarEventDTO } from '../model/CalendarEventDTO';

const props = defineProps({
  events: {
    type: Array as PropType<CalendarEventDTO[]>,
    required: true,
  },
});

// 내부 이벤트 관리
const internalEvents = ref<CalendarEventDTO[]>([]);

// 

watch(() => props.events, (newEvents) => {
  internalEvents.value = [...newEvents]
}, { immediate: true})

const handleEventClick = (info: EventClickArg) => {
  console.log('Event clicked:', info.event); // 이벤트 제목
};

const calendarOptions = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin],
  initialView: 'dayGridMonth',
  events: internalEvents.value,
  editable: true,
  selectable: true, // 날짜 선택 가능하도록,
  locale: 'ko',
  height: 'auto',
  displayEventTime: false,
  eventClick: handleEventClick,
}))
</script>

<template>
  <div class="calendar-container">
    <FullCalendar
    class="calendar"
    :options="calendarOptions"
    />
  </div>
</template>
<style scoped>
.calendar-container {
  width: 100%;
  height: 100%;
}

.calendar {
  height: 700px;
}
</style>