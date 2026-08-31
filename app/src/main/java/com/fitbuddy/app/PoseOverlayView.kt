package com.fitbuddy.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.fitbuddy.app.network.PosePointDto

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<PosePointDto>? = null

    // 프론트 카메라일 때 좌우 반전 여부
    private var isFrontCamera: Boolean = true

    fun setIsFrontCamera(front: Boolean) {
        isFrontCamera = front
    }

    fun updatePose(newPoints: List<PosePointDto>) {
        Log.d("POSE_OVERLAY", "draw points: ${newPoints.size}")
        points = newPoints
        invalidate()
    }

    private val jointPaint = Paint().apply {
        color = 0xFFFFD700.toInt()      // 노란 점
        strokeWidth = 8f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bonePaint = Paint().apply {
        color = 0xFFFFD700.toInt()
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Mediapipe 기준 관절 연결 예시
    private val bones = listOf(
        11 to 12, // 어깨
        11 to 23, // 왼쪽 몸통
        12 to 24, // 오른쪽 몸통
        23 to 24, // 골반
        11 to 13, 13 to 15,  // 왼팔
        12 to 14, 14 to 16,  // 오른팔
        23 to 25, 25 to 27,  // 왼다리
        24 to 26, 26 to 28   // 오른다리
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pts = points ?: return
        if (pts.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // id → point 매핑
        val map = pts.associateBy { it.id }

        // 뼈대 그리기
        for ((a, b) in bones) {
            val p1 = map[a] ?: continue
            val p2 = map[b] ?: continue
            if (p1.score < 0.3f || p2.score < 0.3f) continue

            val x1Norm = if (isFrontCamera) 1f - p1.x else p1.x
            val x2Norm = if (isFrontCamera) 1f - p2.x else p2.x

            val x1 = x1Norm * w
            val y1 = p1.y * h
            val x2 = x2Norm * w
            val y2 = p2.y * h

            canvas.drawLine(x1, y1, x2, y2, bonePaint)
        }

        // 관절 점 찍기
        for (p in pts) {
            if (p.score < 0.3f) continue

            val xNorm = if (isFrontCamera) 1f - p.x else p.x
            val x = xNorm * w
            val y = p.y * h
            canvas.drawCircle(x, y, 6f, jointPaint)
        }
    }
}
