/**
 * Chart.js Integration for ContractIQ Dashboard
 * Uses actual backend metrics to render interactive charts.
 */
const AppCharts = (() => {
  let categoryChartInstance = null;
  let typeChartInstance = null;

  const colorPalette = [
    '#38bdf8', // Cyan
    '#818cf8', // Indigo
    '#a855f7', // Violet
    '#10b981', // Emerald
    '#f59e0b', // Amber
    '#f43f5e', // Rose
    '#64748b'  // Slate
  ];

  function renderCategoryChart(canvasElement, categoryMap) {
    if (!canvasElement) return;
    const ctx = canvasElement.getContext('2d');
    if (categoryChartInstance) {
      categoryChartInstance.destroy();
    }

    const labels = Object.keys(categoryMap || {});
    const data = Object.values(categoryMap || {});

    if (labels.length === 0) {
      labels.push('No Obligations');
      data.push(1);
    }

    categoryChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: colorPalette.slice(0, labels.length),
          borderColor: 'rgba(15, 23, 42, 0.8)',
          borderWidth: 2,
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: '#94a3b8',
              font: { family: 'Inter', size: 12 },
              padding: 16
            }
          },
          tooltip: {
            backgroundColor: '#0f172a',
            titleColor: '#f8fafc',
            bodyColor: '#94a3b8',
            borderColor: 'rgba(255, 255, 255, 0.1)',
            borderWidth: 1,
            padding: 12
          }
        },
        cutout: '68%'
      }
    });
  }

  function renderContractTypeChart(canvasElement, typeMap) {
    if (!canvasElement) return;
    const ctx = canvasElement.getContext('2d');
    if (typeChartInstance) {
      typeChartInstance.destroy();
    }

    const labels = Object.keys(typeMap || {});
    const data = Object.values(typeMap || {});

    if (labels.length === 0) {
      labels.push('No Contracts');
      data.push(0);
    }

    typeChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Contracts',
          data: data,
          backgroundColor: 'rgba(56, 189, 248, 0.65)',
          borderColor: '#38bdf8',
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#94a3b8', font: { family: 'Inter', size: 11 } }
          },
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1, color: '#94a3b8', font: { family: 'Inter' } },
            grid: { color: 'rgba(255, 255, 255, 0.05)' }
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            padding: 12
          }
        }
      }
    });
  }

  return {
    renderCategoryChart,
    renderContractTypeChart
  };
})();
