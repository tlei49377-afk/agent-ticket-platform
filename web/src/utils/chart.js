import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * ECharts 的固定三步：拿一个 div、init、setOption。
 * 这里把 resize 和 dispose 一起包掉，页面里就只关心“配置对象长什么样”。
 *
 *   const { el, render } = useChart(() => ({ series: [...] }))
 *   <div ref="el" style="height: 280px"></div>
 *   render()   // 数据回来后调一次
 */
export function useChart(buildOption) {
  const el = ref()
  let chart = null

  const render = () => {
    if (!el.value) return
    if (!chart || chart.isDisposed()) chart = echarts.init(el.value)
    // notMerge = true：换数据时整体替换，避免上一份数据的 series 残留
    chart.setOption(buildOption(), true)
  }

  const resize = () => {
    if (chart && !chart.isDisposed()) chart.resize()
  }

  onMounted(() => window.addEventListener('resize', resize))

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    if (chart && !chart.isDisposed()) chart.dispose()
    chart = null
  })

  return { el, render, resize }
}
