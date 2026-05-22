package com.abao.speaking

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.abao.speaking.audio.SpeechPlayer
import com.abao.speaking.data.AppData
import com.abao.speaking.databinding.ActivityMainBinding
import com.abao.speaking.databinding.ItemDialogueMessageBinding
import com.abao.speaking.databinding.ItemMetricBinding
import com.abao.speaking.logic.WebScript
import com.abao.speaking.model.DialogueMessage
import com.abao.speaking.model.ScoreResult
import com.abao.speaking.ui.PandaIdleAnimator
import com.abao.speaking.ui.UiLayoutHelper
import com.abao.speaking.ui.WordCardAdapter
import com.abao.speaking.util.AssetImageLoader
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQ_RECORD_AUDIO = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private val recorder = WavRecorder()
    private val nlsClient = AliyunNlsClient()
    private val executor = Executors.newSingleThreadExecutor()

    private var currentScenario = AppData.scenarios.first()
    private var recognizing = false
    private var startedAt = 0L
    private lateinit var speechPlayer: SpeechPlayer
    private val dialogueMessages = mutableListOf<DialogueMessage>()
    private var suppressSpinnerEvent = true
    private var currentTab = Tab.WARMUP
    private var pendingFinishAfterRecognize = false

    /** include 布局生成的嵌套 Binding，根视图用 [feedback.root] */
    private val feedback get() = binding.feedbackPanel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechPlayer = SpeechPlayer(this)
        binding.warmupSectionTitle.sectionTitleText.setText(R.string.warmup_title)
        binding.challengeSectionTitle.sectionTitleText.setText(R.string.challenge_title)
        setupScenarioSpinner()
        setupWarmupGrid()
        setupTabs()
        setupActions()
        selectScenario(currentScenario.id, fromSpinner = false)
        switchTab(Tab.WARMUP)
        binding.scenarioSelect.post { suppressSpinnerEvent = false }
        ensureRecordPermission()
        PandaIdleAnimator.start(binding.pandaAvatar)
        UiLayoutHelper.applyMainLayout(binding, resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        UiLayoutHelper.applyMainLayout(binding, newConfig)
    }

    override fun onDestroy() {
        PandaIdleAnimator.stop(binding.pandaAvatar)
        speechPlayer.shutdown()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "需要麦克风权限才能录音", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupScenarioSpinner() {
        val titles = AppData.scenarios.map { it.title }
        binding.scenarioSelect.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, titles)
        binding.scenarioSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerEvent) return
                selectScenario(AppData.scenarios[position].id, fromSpinner = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupWarmupGrid() {
        binding.wordGrid.layoutManager = GridLayoutManager(this, 3)
        binding.wordGrid.adapter = WordCardAdapter(AppData.warmupWords) { speak(it) }
    }

    private fun setupTabs() {
        binding.tabWarmup.setOnClickListener { switchTab(Tab.WARMUP) }
        binding.tabPractice.setOnClickListener { switchTab(Tab.PRACTICE) }
        binding.tabChallenge.setOnClickListener { switchTab(Tab.CHALLENGE) }
    }

    private fun setupActions() {
        binding.recordButton.setOnClickListener { toggleRecording() }
        binding.finishButton.setOnClickListener { finishReading() }
        binding.speakPromptButton.setOnClickListener { speak(currentScenario.opening) }
        binding.hintButton.setOnClickListener { showHint() }
        binding.translateButton.setOnClickListener { showTranslation() }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        val tabs = listOf(binding.tabWarmup, binding.tabPractice, binding.tabChallenge)
        val panels = listOf(binding.panelWarmup, binding.panelPractice, binding.panelChallenge)
        tabs.forEachIndexed { index, button ->
            val active = Tab.entries[index] == tab
            button.backgroundTintList = null
            button.setBackgroundResource(if (active) R.drawable.bg_tab_active else R.drawable.bg_tab_inactive)
            button.setTextColor(
                ContextCompat.getColor(this, if (active) R.color.white else R.color.deep_blue)
            )
        }
        panels.forEachIndexed { index, panel ->
            panel.visibility = if (Tab.entries[index] == tab) View.VISIBLE else View.GONE
        }
        updateAgentHint()
        val avatarFile = if (tab == Tab.CHALLENGE) "panda-suit.png" else "panda-hanfu.png"
        AssetImageLoader.load(this, avatarFile, binding.pandaAvatar, binding.pandaAvatarFallback)
        if (tab == Tab.CHALLENGE) {
            AssetImageLoader.load(this, "challenge.png", binding.challengeImage, null)
        }
    }

    private fun updateAgentHint() {
        binding.agentHint.text = when (currentTab) {
            Tab.PRACTICE -> "当前情景：${currentScenario.title}"
            else -> getString(R.string.agent_hint_default)
        }
    }

    private fun selectScenario(id: String, fromSpinner: Boolean) {
        currentScenario = AppData.scenarios.find { it.id == id } ?: AppData.scenarios.first()
        val index = AppData.scenarios.indexOf(currentScenario)
        if (!fromSpinner && binding.scenarioSelect.selectedItemPosition != index) {
            suppressSpinnerEvent = true
            binding.scenarioSelect.setSelection(index)
            binding.scenarioSelect.post { suppressSpinnerEvent = false }
        }
        binding.scenarioTitle.text = currentScenario.title
        updateAgentHint()
        binding.studentInput.setText("")
        binding.studentInput.hint = getString(R.string.input_placeholder)
        feedback.root.visibility = View.GONE
        dialogueMessages.clear()
        dialogueMessages.add(DialogueMessage("ai", currentScenario.opening, currentScenario.zh))
        renderDialogue()
    }

    private fun renderDialogue() {
        binding.dialogueContainer.removeAllViews()
        dialogueMessages.forEach { message ->
            binding.dialogueContainer.addView(createMessageView(message))
        }
        binding.dialogueScroll.post { binding.dialogueScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun appendMessage(role: String, text: String, sub: String = "") {
        dialogueMessages.add(DialogueMessage(role, text, sub))
        binding.dialogueContainer.addView(createMessageView(DialogueMessage(role, text, sub)))
        binding.dialogueScroll.post {
            binding.dialogueScroll.fullScroll(View.FOCUS_DOWN)
            binding.dialogueContainer.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        }
    }

    private fun createMessageView(message: DialogueMessage): View {
        val isStudent = message.role == "student"
        val item = ItemDialogueMessageBinding.inflate(layoutInflater, binding.dialogueContainer, false)
        val maxBubbleWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        item.root.setBackgroundResource(
            if (isStudent) R.drawable.bg_student_message else R.drawable.bg_ai_message
        )
        item.messageText.text = message.text
        if (message.sub.isNotEmpty()) {
            item.messageSub.visibility = View.VISIBLE
            item.messageSub.text = message.sub
        } else {
            item.messageSub.visibility = View.GONE
        }
        item.root.measure(
            View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        item.root.layoutParams = LinearLayout.LayoutParams(
            item.root.measuredWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (isStudent) Gravity.END else Gravity.START
        }
        return item.root
    }

    private fun toggleRecording() {
        if (recognizing) {
            recognizing = false
            binding.recordButton.setBackgroundResource(R.drawable.bg_teal_button)
            binding.recordButton.isEnabled = false
            binding.recordButton.text = getString(R.string.record_recognizing)
            binding.studentInput.hint = getString(R.string.recognizing_placeholder)
            executor.execute {
                try {
                    val wav = recorder.stop()
                    val text = nlsClient.recognize(wav)
                    runOnUiThread { onRecognized(text) }
                } catch (error: Exception) {
                    runOnUiThread { onRecognizeError(error.message ?: "识别失败") }
                }
            }
            return
        }
        ensureRecordPermission()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            binding.studentInput.requestFocus()
            return
        }
        try {
            binding.studentInput.setText("")
            startedAt = System.currentTimeMillis()
            recognizing = true
            binding.recordButton.setBackgroundResource(R.drawable.bg_danger_button)
            binding.recordButton.text = getString(R.string.record_stop)
            binding.studentInput.hint = getString(R.string.recording_placeholder)
            recorder.start(this)
        } catch (error: Exception) {
            onRecognizeError(error.message ?: "录音失败")
        }
    }

    private fun onRecognized(text: String) {
        recognizing = false
        binding.recordButton.isEnabled = true
        binding.recordButton.setBackgroundResource(R.drawable.bg_teal_button)
        binding.recordButton.text = getString(R.string.record_start)
        binding.studentInput.setText(text)
        binding.studentInput.hint = if (text.isBlank()) {
            "未识别到内容，请重试或直接输入英文。"
        } else {
            "识别完成，可继续修改或点击完成朗读。"
        }
        if (pendingFinishAfterRecognize) {
            pendingFinishAfterRecognize = false
            doFinishReading()
        }
    }

    private fun onRecognizeError(message: String) {
        pendingFinishAfterRecognize = false
        recognizing = false
        binding.recordButton.isEnabled = true
        binding.recordButton.setBackgroundResource(R.drawable.bg_teal_button)
        binding.recordButton.text = getString(R.string.record_start)
        binding.studentInput.hint = message + getString(R.string.recognize_error_suffix)
    }

    private fun finishReading() {
        if (recognizing) {
            pendingFinishAfterRecognize = true
            toggleRecording()
            return
        }
        doFinishReading()
    }

    private fun doFinishReading() {
        var answer = binding.studentInput.text.toString().trim()
        if (answer.isEmpty()) {
            binding.studentInput.setText(currentScenario.target)
            answer = currentScenario.target
        }
        appendMessage("student", answer)
        appendMessage("ai", currentScenario.next, "阿宝会根据你的回答继续追问。")
        val elapsed = maxOf(5.0, (System.currentTimeMillis() - (startedAt.takeIf { it > 0 } ?: (System.currentTimeMillis() - 12000))).toDouble() / 1000.0)
        val result = WebScript.scoreAnswer(answer, currentScenario.target, elapsed)
        renderFeedback(result, answer)
        speak(currentScenario.next)
        feedback.root.post {
            feedback.root.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        }
    }

    private fun renderFeedback(result: ScoreResult, answer: String) {
        feedback.root.visibility = View.VISIBLE
        UiLayoutHelper.applyMainLayout(binding, resources.configuration)
        feedback.totalScore.text = result.total.toString()
        feedback.speedValue.text = result.wpm.toString()
        feedback.speedNeedle.rotation = WebScript.speedNeedleDegrees(result.wpm)
        feedback.analysisText.text = WebScript.buildAnalysis(result)
        val correctionLabel = getString(R.string.correction_prefix)
        val correctionBody = WebScript.suggestCorrection(answer, currentScenario.target)
        feedback.correctionText.text = SpannableStringBuilder(correctionLabel + correctionBody).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, correctionLabel.length, 0)
        }

        feedback.metricList.removeAllViews()
        AppData.metrics.forEach { name ->
            val metricBinding = ItemMetricBinding.inflate(layoutInflater, feedback.metricList, false)
            val value = result.values[name] ?: 0
            metricBinding.metricName.text = name
            metricBinding.metricValue.text = value.toString()
            metricBinding.metricBar.progress = value
            feedback.metricList.addView(metricBinding.root)
        }
        feedback.root.post {
            feedback.root.parent?.requestLayout()
        }
    }

    private fun showHint() {
        appendMessage(
            "ai",
            "You can use: ${currentScenario.hints.joinToString(" / ")}",
            "这些短语可以直接放进回答里。"
        )
    }

    private fun showTranslation() {
        appendMessage("ai", currentScenario.zh, "中文提示")
    }

    private fun speak(text: String) {
        speechPlayer.speak(text)
    }

    private fun ensureRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) return
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQ_RECORD_AUDIO
        )
    }

    private enum class Tab { WARMUP, PRACTICE, CHALLENGE }
}
