# 📱 Android Stories Integration Guide

## Огляд реалізації

Stories функціонал для Android додатку **повністю інтегрований** з існуючою архітектурою проекту.

## ✅ Що вже зроблено

### 1. **Моделі даних** (`data/model/Story.kt`)
- ✅ `Story` - основна модель story
- ✅ `StoryMedia` - медіа файли (фото/відео)
- ✅ `StoryUser` - дані користувача
- ✅ `StoryComment` - коментарі
- ✅ `StoryReactions` - реакції
- ✅ `StoryViewer` - перегляди
- ✅ `StoryLimits` - обмеження за підпискою
- ✅ Всі Response моделі для API

**Файл:** `app/src/main/java/com/worldmates/messenger/data/model/Story.kt`

### 2. **API Сервіс** (`network/StoriesApiService.kt`)
- ✅ `createStory()` - створення story
- ✅ `getStories()` - список stories
- ✅ `getStoryById()` - story за ID
- ✅ `getUserStories()` - stories користувача
- ✅ `deleteStory()` - видалення
- ✅ `getStoryViews()` - перегляди
- ✅ `reactToStory()` - реакції
- ✅ `muteStory()` - приглушення
- ✅ `createStoryComment()` - створення коментаря
- ✅ `getStoryComments()` - отримання коментарів
- ✅ `deleteStoryComment()` - видалення коментаря

**Файл:** `app/src/main/java/com/worldmates/messenger/network/StoriesApiService.kt`

### 3. **Repository** (`data/repository/StoryRepository.kt`)
- ✅ Повна реалізація Repository pattern
- ✅ StateFlow для реактивних даних
- ✅ Інтеграція з MediaUploader
- ✅ Перевірка обмежень підписки
- ✅ Кешування та обробка помилок
- ✅ Автоматична перевірка протермінованих stories

**Файл:** `app/src/main/java/com/worldmates/messenger/data/repository/StoryRepository.kt`

### 4. **ViewModel** (`ui/stories/StoryViewModel.kt`)
- ✅ Повна бізнес-логіка для Stories
- ✅ Автоматична перевірка обмежень
- ✅ Обробка всіх операцій
- ✅ Реактивні StateFlow для UI
- ✅ Валідація перед створенням story

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/stories/StoryViewModel.kt`

---

## 🔧 Що потрібно доробити (UI частина)

Для завершення інтеграції потрібно створити UI компоненти. Нижче детальні інструкції.

### Крок 1: Створення Activity для перегляду Stories

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/stories/StoryViewerActivity.kt`

```kotlin
package com.worldmates.messenger.ui.stories

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.worldmates.messenger.databinding.ActivityStoryViewerBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StoryViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoryViewerBinding
    private val viewModel: StoryViewModel by viewModels()

    private var currentStoryIndex = 0
    private var stories: List<Story> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Отримати ID story або користувача з intent
        val storyId = intent.getLongExtra("story_id", -1)
        val userId = intent.getLongExtra("user_id", -1)

        when {
            storyId != -1L -> viewModel.loadStoryById(storyId)
            userId != -1L -> viewModel.loadUserStories(userId)
            else -> viewModel.loadStories()
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.stories.collect { storyList ->
                stories = storyList
                if (stories.isNotEmpty()) {
                    displayStory(stories[currentStoryIndex])
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentStory.collect { story ->
                story?.let { displayStory(it) }
            }
        }
    }

    private fun displayStory(story: Story) {
        // Відобразити story
        binding.apply {
            // Завантажити медіа (фото або відео)
            story.videos?.firstOrNull()?.let { video ->
                // Відео
                loadVideo(video.filename)
            } ?: story.images?.firstOrNull()?.let { image ->
                // Фото
                loadImage(image.filename)
            }

            // Інформація про користувача
            tvUsername.text = story.userData?.getFullName()
            // ... додаткова логіка
        }
    }

    private fun setupListeners() {
        // Свайп для наступної/попередньої story
        // Тап для паузи/продовження
        // Кнопка коментарів
        // Кнопка реакцій
    }
}
```

### Крок 2: Створення Layout для Story Viewer

**Файл:** `app/src/main/res/layout/activity_story_viewer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black">

    <!-- Прогрес бар для stories -->
    <com.worldmates.messenger.ui.stories.widgets.StoryProgressView
        android:id="@+id/storyProgress"
        android:layout_width="match_parent"
        android:layout_height="2dp"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toTopOf="parent"/>

    <!-- Медіа контейнер (ImageView або VideoView) -->
    <FrameLayout
        android:id="@+id/mediaContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/storyProgress"
        app:layout_constraintBottom_toTopOf="@id/bottomControls">

        <ImageView
            android:id="@+id/ivStoryImage"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="centerCrop"
            android:visibility="gone"/>

        <VideoView
            android:id="@+id/vvStoryVideo"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"/>
    </FrameLayout>

    <!-- Інфо про користувача -->
    <LinearLayout
        android:id="@+id/userInfo"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        app:layout_constraintTop_toBottomOf="@id/storyProgress">

        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivUserAvatar"
            android:layout_width="40dp"
            android:layout_height="40dp"/>

        <TextView
            android:id="@+id/tvUsername"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:textColor="@color/white"
            android:textStyle="bold"/>

        <TextView
            android:id="@+id/tvTimeAgo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:textColor="@color/white_alpha_70"/>
    </LinearLayout>

    <!-- Кнопки управління (коментарі, реакції) -->
    <LinearLayout
        android:id="@+id/bottomControls"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <EditText
            android:id="@+id/etComment"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_weight="1"
            android:hint="Коментар..."
            android:textColorHint="@color/white_alpha_70"
            android:textColor="@color/white"/>

        <ImageButton
            android:id="@+id/btnSendComment"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_send"/>

        <ImageButton
            android:id="@+id/btnReactions"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_reactions"/>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Крок 3: Створення Adapter для списку Stories

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/stories/adapters/StoriesAdapter.kt`

```kotlin
package com.worldmates.messenger.ui.stories.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.worldmates.messenger.data.model.Story
import com.worldmates.messenger.databinding.ItemStoryBinding

class StoriesAdapter(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<Story, StoriesAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StoryViewHolder(binding, onStoryClick)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StoryViewHolder(
        private val binding: ItemStoryBinding,
        private val onStoryClick: (Story) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(story: Story) {
            binding.apply {
                // Аватар користувача
                Glide.with(root.context)
                    .load(story.userData?.avatar)
                    .into(ivUserAvatar)

                // Ім'я користувача
                tvUsername.text = story.userData?.getFullName()

                // Індикатор перегляду
                borderView.setViewed(story.isViewed > 0)

                // Клік по story
                root.setOnClickListener {
                    onStoryClick(story)
                }
            }
        }
    }

    private class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem == newItem
        }
    }
}
```

### Крок 4: Layout для елемента списку Stories

**Файл:** `app/src/main/res/layout/item_story.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="80dp"
    android:layout_height="wrap_content"
    android:padding="8dp">

    <!-- Градієнтна рамка (непереглянута story) -->
    <com.worldmates.messenger.ui.stories.widgets.StoryBorderView
        android:id="@+id/borderView"
        android:layout_width="70dp"
        android:layout_height="70dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Аватар користувача -->
    <de.hdodenhof.circleimageview.CircleImageView
        android:id="@+id/ivUserAvatar"
        android:layout_width="64dp"
        android:layout_height="64dp"
        app:layout_constraintTop_toTopOf="@id/borderView"
        app:layout_constraintBottom_toBottomOf="@id/borderView"
        app:layout_constraintStart_toStartOf="@id/borderView"
        app:layout_constraintEnd_toEndOf="@id/borderView"/>

    <!-- Ім'я користувача -->
    <TextView
        android:id="@+id/tvUsername"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:textSize="12sp"
        android:maxLines="1"
        android:ellipsize="end"
        android:gravity="center"
        app:layout_constraintTop_toBottomOf="@id/borderView"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 📲 Інтеграція в головний екран (MainActivity або ChatsActivity)

### Додати RecyclerView для Stories

У layout головного екрану додати горизонтальний RecyclerView:

```xml
<!-- У activity_main.xml або fragment_chats.xml -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvStories"
    android:layout_width="match_parent"
    android:layout_height="100dp"
    android:orientation="horizontal"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"/>
```

### Ініціалізація в Activity/Fragment

```kotlin
class ChatsActivity : AppCompatActivity() {

    private val storyViewModel: StoryViewModel by viewModels()
    private lateinit var storiesAdapter: StoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація адаптера
        storiesAdapter = StoriesAdapter { story ->
            // Відкрити StoryViewerActivity
            val intent = Intent(this, StoryViewerActivity::class.java)
            intent.putExtra("story_id", story.id)
            startActivity(intent)
        }

        binding.rvStories.adapter = storiesAdapter

        // Спостереження за stories
        lifecycleScope.launch {
            storyViewModel.stories.collect { stories ->
                storiesAdapter.submitList(stories)
            }
        }
    }
}
```

---

## 🎬 Створення нової Story

### Додати кнопку "Створити Story"

```kotlin
binding.btnCreateStory.setOnClickListener {
    // Відкрити галерею або камеру
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*,video/*"
    startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_PICK_MEDIA && resultCode == RESULT_OK) {
        val mediaUri = data?.data ?: return

        // Визначити тип файлу
        val mimeType = contentResolver.getType(mediaUri)
        val fileType = when {
            mimeType?.startsWith("image/") == true -> "image"
            mimeType?.startsWith("video/") == true -> "video"
            else -> return
        }

        // Для відео отримати тривалість
        val duration = if (fileType == "video") {
            getVideoDuration(mediaUri) // Реалізувати цю функцію
        } else null

        // Створити story
        storyViewModel.createStory(
            mediaUri = mediaUri,
            fileType = fileType,
            videoDuration = duration
        )
    }
}
```

---

## 🔒 Обмеження підписки

Обмеження автоматично перевіряються у `StoryViewModel`:

```kotlin
// Перевірка перед створенням
val canCreate = storyViewModel.canCreateStory()
if (!canCreate) {
    // Показати діалог про обмеження
    showSubscriptionDialog()
}

// Отримати поточні обмеження
val limits = storyViewModel.userLimits.value
```

### Діалог про оформлення підписки

```kotlin
fun showSubscriptionDialog() {
    AlertDialog.Builder(this)
        .setTitle("Обмеження досягнуто")
        .setMessage(
            "Безкоштовні користувачі можуть мати максимум 2 активні stories.\n\n" +
            "Оформіть підписку для:\n" +
            "• До 15 активних stories\n" +
            "• Відео до 45 секунд\n" +
            "• Зберігання 48 годин"
        )
        .setPositiveButton("Оформити підписку") { _, _ ->
            // Відкрити екран підписки
        }
        .setNegativeButton("Пізніше", null)
        .show()
}
```

---

## 🧩 Додаткові компоненти (опціонально)

### 1. StoryProgressView - прогрес бар для stories

```kotlin
class StoryProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var storiesCount = 1
    private var currentIndex = 0
    private var progress = 0f

    fun setStoriesCount(count: Int) {
        storiesCount = count
        invalidate()
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    // ... малювання прогресу
}
```

### 2. StoryBorderView - градієнтна рамка

```kotlin
class StoryBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var isViewed = false

    fun setViewed(viewed: Boolean) {
        isViewed = viewed
        invalidate()
    }

    // ... малювання градієнта
}
```

---

## 📝 Додаткові функції

### Коментарі

```kotlin
// Відобразити коментарі
binding.btnShowComments.setOnClickListener {
    showCommentsBottomSheet(storyId)
}

// Створити коментар
binding.btnSendComment.setOnClickListener {
    val text = binding.etComment.text.toString()
    if (text.isNotEmpty()) {
        storyViewModel.createComment(storyId, text)
        binding.etComment.setText("")
    }
}
```

### Реакції

```kotlin
// Показати селектор реакцій
binding.btnReactions.setOnClickListener {
    showReactionsSelector { reaction ->
        storyViewModel.reactToStory(storyId, reaction)
    }
}
```

### Перегляди (тільки для власних stories)

```kotlin
if (story.isOwner) {
    binding.btnViewers.visibility = View.VISIBLE
    binding.btnViewers.setOnClickListener {
        showViewersDialog(story.id)
    }
}
```

---

## 🚀 Запуск та тестування

1. **Застосувати міграцію БД:**
   ```bash
   mysql -u username -p database_name < api-server-files/sql-DB-newver/migration_story_comments.sql
   ```

2. **Налаштувати cron для автовидалення:**
   ```bash
   0 * * * * php /path/to/api-server-files/api/v2/cron/delete_expired_stories.php
   ```

3. **Запустити Android додаток**

4. **Тестування:**
   - Створення story (фото/відео)
   - Перегляд stories
   - Коментування
   - Реакції
   - Перегляд списку переглядів (для власних stories)
   - Перевірка обмежень підписки

---

## 📚 Корисні посилання

- [API Documentation](./STORIES_API_DOCUMENTATION.md)
- [Server Implementation](./api-server-files/api/v2/endpoints/)
- [Android Models](./app/src/main/java/com/worldmates/messenger/data/model/Story.kt)
- [Repository](./app/src/main/java/com/worldmates/messenger/data/repository/StoryRepository.kt)
- [ViewModel](./app/src/main/java/com/worldmates/messenger/ui/stories/StoryViewModel.kt)

---

## ⚠️ Важливо

1. **ViewBinding:** Проект використовує ViewBinding, переконайтесь що він увімкнений у `build.gradle`
2. **Glide:** Для завантаження зображень використовується Glide
3. **Permissions:** Додайте дозволи для камери та галереї у `AndroidManifest.xml`
4. **ProGuard:** Додайте правила для збереження моделей при обфускації

---

## 🎨 Рекомендації по дизайну

- Використовуйте Instagram-style UI для Stories
- Додайте анімації переходів між stories
- Реалізуйте автоплей для відео
- Додайте індикатори прогресу
- Використовуйте Material Design компоненти

---

Успіхів з інтеграцією! 🚀
