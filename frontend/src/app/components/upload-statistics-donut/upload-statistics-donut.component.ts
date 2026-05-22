import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FileUploadStatsService, FileUploadStats, UploadStatus } from '../../services/file-upload-stats.service';
import { DashboardOverview } from '../../models/dashboard-overview.model';

@Component({
  selector: 'app-upload-statistics-donut',
  templateUrl: './upload-statistics-donut.component.html',
  styleUrls: ['./upload-statistics-donut.component.scss']
})
export class UploadStatisticsDonutComponent implements OnInit, OnChanges {
  
  @Input() overview: DashboardOverview | null = null;

  stats: FileUploadStats | null = null;
  loading = false;
  error = '';
  hoveredStatus: string | null = null;
  
  constructor(private fileUploadStatsService: FileUploadStatsService) { }

  ngOnInit(): void {
    this.updateStats();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['overview']) {
      this.updateStats();
    }
  }

  private updateStats(): void {
    if (this.overview) {
      this.stats = this.fileUploadStatsService.transformOverviewToStats(this.overview);
      this.loading = false;
      this.error = '';
    } else {
      this.stats = null;
    }
  }

  getSegmentAngles() {
    if (!this.stats || this.stats.total === 0) return [];
    const gapDegrees = 2; // small gap between segments for a segmented look
    let currentAngle = -90; // Start at top
    return this.stats.statuses
      .filter((status: UploadStatus) => status.count > 0)
      .map((status: UploadStatus) => {
        const rawAngle = (status.count / this.stats!.total) * 360;
        const startAngle = currentAngle + gapDegrees / 2;
        const endAngle = currentAngle + rawAngle - gapDegrees / 2;
        // advance currentAngle by the raw angle (including gap)
        currentAngle += rawAngle;

        return {
          name: status.name,
          count: status.count,
          percentage: status.percentage,
          color: status.color,
          startAngle,
          endAngle,
          midAngle: (startAngle + endAngle) / 2
        };
      });
  }

  /**
   * Generates SVG path for donut segment
   */
  generatePath(startAngle: number, endAngle: number): string {
    // Larger radius and thinner inner radius to create a thicker, more prominent ring
    const radius = 90;
    const innerRadius = 50;
    
    const toRad = (deg: number) => (deg * Math.PI) / 180;
    
    const x1 = 100 + radius * Math.cos(toRad(startAngle));
    const y1 = 100 + radius * Math.sin(toRad(startAngle));
    const x2 = 100 + radius * Math.cos(toRad(endAngle));
    const y2 = 100 + radius * Math.sin(toRad(endAngle));
    
    const x3 = 100 + innerRadius * Math.cos(toRad(endAngle));
    const y3 = 100 + innerRadius * Math.sin(toRad(endAngle));
    const x4 = 100 + innerRadius * Math.cos(toRad(startAngle));
    const y4 = 100 + innerRadius * Math.sin(toRad(startAngle));
    
    const largeArc = endAngle - startAngle > 180 ? 1 : 0;
    
    const path = [
      `M ${x1} ${y1}`,
      `A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2}`,
      `L ${x3} ${y3}`,
      `A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${x4} ${y4}`,
      `Z`
    ].join(' ');
    
    return path;
  }

  /**
   * Handles mouse enter on legend item
   */
  onLegendHover(status: string): void {
    this.hoveredStatus = status;
  }

  /**
   * Handles mouse leave on legend item
   */
  onLegendLeave(): void {
    this.hoveredStatus = null;
  }

  /**
   * Gets segment opacity based on hover state
   */
  getSegmentOpacity(statusName: string): number {
    if (this.hoveredStatus === null) {
      return 1;
    }
    return this.hoveredStatus === statusName ? 1 : 0.4;
  }
}
