package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Dataset
import com.example.data.model.StudyItem
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetScreen(
    viewModel: StudyViewModel
) {
    val datasets by viewModel.datasets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.studyItems.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Manage", "Operations", "Import / Export")

    // Modals & Forms State
    var showAddDatasetDialog by remember { mutableStateOf(false) }
    var newDatasetName by remember { mutableStateOf("") }
    var newDatasetDesc by remember { mutableStateOf("") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedDatasetForCategory by remember { mutableStateOf<Dataset?>(null) }
    var newCategoryName by remember { mutableStateOf("") }

    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedCategoryForCard by remember { mutableStateOf<Category?>(null) }
    var newCardQuestion by remember { mutableStateOf("") }
    var newCardAnswer by remember { mutableStateOf("") }
    var newCardExplanation by remember { mutableStateOf("") }
    var newCardTags by remember { mutableStateOf("") }
    var newCardDiff by remember { mutableStateOf("Medium") }
    var newCardImageUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dataset Manager", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth().testTag("dataset_tab_row")
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("dataset_tab_$idx")
                    )
                }
            }

            when (activeTab) {
                0 -> {
                    // Manage tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showAddDatasetDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("add_dataset_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Study Dataset")
                            }
                        }

                        if (datasets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No datasets present. Press the button above to create one!")
                                }
                            }
                        }

                        items(datasets) { dataset ->
                            val datasetCategories = categories.filter { it.datasetId == dataset.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dataset_card_${dataset.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dataset.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (!dataset.description.isNullOrEmpty()) {
                                                Text(
                                                    text = dataset.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = {
                                                selectedDatasetForCategory = dataset
                                                showAddCategoryDialog = true
                                            }, modifier = Modifier.testTag("add_category_btn_${dataset.id}")) {
                                                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.deleteDataset(dataset) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Dataset", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${datasetCategories.size} Categories in total",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    datasetCategories.forEach { category ->
                                        val categoryItems = items.filter { it.categoryId == category.id }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${categoryItems.size} flashcards",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(onClick = {
                                                    selectedCategoryForCard = category
                                                    showAddCardDialog = true
                                                }, modifier = Modifier.testTag("add_item_btn_${category.id}")) {
                                                    Icon(imageVector = Icons.Default.PostAdd, contentDescription = "Add Card", tint = MaterialTheme.colorScheme.secondary)
                                                }
                                                IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Operations tab
                    OperationsPanel(viewModel, datasets, categories, items)
                }
                2 -> {
                    // Import/Export tab
                    ImportExportPanel(viewModel, categories)
                }
            }
        }

        // --- Dialogs ---
        if (showAddDatasetDialog) {
            AlertDialog(
                onDismissRequest = { showAddDatasetDialog = false },
                title = { Text("Create Dataset") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newDatasetName,
                            onValueChange = { newDatasetName = it },
                            label = { Text("Dataset Name (e.g. Current Affairs)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_dataset_name_input")
                        )
                        OutlinedTextField(
                            value = newDatasetDesc,
                            onValueChange = { newDatasetDesc = it },
                            label = { Text("Overview Description (Optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_dataset_desc_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newDatasetName.isNotBlank()) {
                                viewModel.addDataset(newDatasetName, newDatasetDesc.ifEmpty { null })
                                newDatasetName = ""
                                newDatasetDesc = ""
                                showAddDatasetDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_dataset_btn")
                    ) {
                        Text("Create")
                    }
                }
            )
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("New Category in '${selectedDatasetForCategory?.name}'") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category (e.g. Synonyms)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank() && selectedDatasetForCategory != null) {
                                viewModel.addCategory(selectedDatasetForCategory!!.id, newCategoryName)
                                newCategoryName = ""
                                showAddCategoryDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_category_btn")
                    ) {
                        Text("Create Category")
                    }
                }
            )
        }

        if (showAddCardDialog) {
            AlertDialog(
                onDismissRequest = { showAddCardDialog = false },
                title = { Text("Add Flashcard to '${selectedCategoryForCard?.name}'") },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = newCardQuestion,
                                onValueChange = { newCardQuestion = it },
                                label = { Text("Question / Term") },
                                modifier = Modifier.fillMaxWidth().testTag("card_question_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = newCardAnswer,
                                onValueChange = { newCardAnswer = it },
                                label = { Text("Answer Definition") },
                                modifier = Modifier.fillMaxWidth().testTag("card_answer_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = newCardExplanation,
                                onValueChange = { newCardExplanation = it },
                                label = { Text("Explanation (Optional Hint)") },
                                modifier = Modifier.fillMaxWidth().testTag("card_ex_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = newCardTags,
                                onValueChange = { newCardTags = it },
                                label = { Text("Tags (delimited by commas)") },
                                modifier = Modifier.fillMaxWidth().testTag("card_tags_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = newCardImageUrl,
                                onValueChange = { newCardImageUrl = it },
                                label = { Text("Image URL or Source Path (Optional)") },
                                modifier = Modifier.fillMaxWidth().testTag("card_image_url_input")
                            )
                        }
                        item {
                            Text("Difficulty:", style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Easy", "Medium", "Hard").forEach { d ->
                                    FilterChip(
                                        selected = newCardDiff == d,
                                        onClick = { newCardDiff = d },
                                        label = { Text(d) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCardQuestion.isNotBlank() && newCardAnswer.isNotBlank() && selectedCategoryForCard != null) {
                                viewModel.addStudyItem(
                                    categoryId = selectedCategoryForCard!!.id,
                                    question = newCardQuestion,
                                    answer = newCardAnswer,
                                    explanation = newCardExplanation.ifEmpty { null },
                                    imageUrl = newCardImageUrl.ifEmpty { null },
                                    tags = newCardTags,
                                    difficulty = newCardDiff
                                )
                                newCardQuestion = ""
                                newCardAnswer = ""
                                newCardExplanation = ""
                                newCardImageUrl = ""
                                newCardTags = ""
                                newCardDiff = "Medium"
                                showAddCardDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_card_btn")
                    ) {
                        Text("Add Card")
                    }
                }
            )
        }
    }
}

@Composable
fun OperationsPanel(
    viewModel: StudyViewModel,
    datasets: List<Dataset>,
    categories: List<Category>,
    items: List<StudyItem>
) {
    var sourceDatasetId by remember { mutableLongStateOf(0L) }
    var targetDatasetId by remember { mutableLongStateOf(0L) }

    var categoryDropdownSource by remember { mutableStateOf(false) }
    var categoryDropdownTarget by remember { mutableStateOf(false) }

    var duplicateDatasetId by remember { mutableLongStateOf(0L) }
    var duplicateDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Duplicate dataset Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Duplicate Dataset", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val dupeName = datasets.find { it.id == duplicateDatasetId }?.name ?: "Select Dataset"
                        OutlinedButton(
                            onClick = { duplicateDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dupeName)
                        }
                        DropdownMenu(expanded = duplicateDropdown, onDismissRequest = { duplicateDropdown = false }) {
                            datasets.forEach { d ->
                                DropdownMenuItem(text = { Text(d.name) }, onClick = {
                                    duplicateDatasetId = d.id
                                    duplicateDropdown = false
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (duplicateDatasetId != 0L) {
                                viewModel.duplicateDataset(duplicateDatasetId)
                            }
                        },
                        enabled = duplicateDatasetId != 0L,
                        modifier = Modifier.fillMaxWidth().testTag("action_duplicate_btn")
                    ) {
                        Text("Execute Duplication")
                    }
                }
            }
        }

        // Merge datasets Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Merge Category Strands", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Merge category templates of Source into Target dataset.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            val name = datasets.find { it.id == sourceDatasetId }?.name ?: "Source"
                            OutlinedButton(onClick = { categoryDropdownSource = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(name, maxLines = 1)
                            }
                            DropdownMenu(expanded = categoryDropdownSource, onDismissRequest = { categoryDropdownSource = false }) {
                                datasets.forEach { d ->
                                    DropdownMenuItem(text = { Text(d.name) }, onClick = {
                                        sourceDatasetId = d.id
                                        categoryDropdownSource = false
                                    })
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            val name = datasets.find { it.id == targetDatasetId }?.name ?: "Target"
                            OutlinedButton(onClick = { categoryDropdownTarget = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(name, maxLines = 1)
                            }
                            DropdownMenu(expanded = categoryDropdownTarget, onDismissRequest = { categoryDropdownTarget = false }) {
                                datasets.forEach { d ->
                                    DropdownMenuItem(text = { Text(d.name) }, onClick = {
                                        targetDatasetId = d.id
                                        categoryDropdownTarget = false
                                    })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (sourceDatasetId != 0L && targetDatasetId != 0L && sourceDatasetId != targetDatasetId) {
                                viewModel.mergeDataset(sourceDatasetId, targetDatasetId)
                            }
                        },
                        enabled = sourceDatasetId != 0L && targetDatasetId != 0L && sourceDatasetId != targetDatasetId,
                        modifier = Modifier.fillMaxWidth().testTag("action_merge_btn")
                    ) {
                        Text("Merge Datasets")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportPanel(
    viewModel: StudyViewModel,
    categories: List<Category>
) {
    val items by viewModel.studyItems.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var inputString by remember { mutableStateOf("") }
    var operationLog by remember { mutableStateOf("") }

    var activeCatTargetId by remember { mutableLongStateOf(0L) }
    var targetDropOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bulk import Section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bulk Importer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paste CSV (Format: question,answer,explanation,tags) or complete JSON structure", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose Import Target Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val name = categories.find { it.id == activeCatTargetId }?.name ?: "Select Target Category"
                        OutlinedButton(onClick = { targetDropOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(name)
                        }
                        DropdownMenu(expanded = targetDropOpen, onDismissRequest = { targetDropOpen = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                    activeCatTargetId = c.id
                                    targetDropOpen = false
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputString,
                        onValueChange = { inputString = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("bulk_import_textfield"),
                        placeholder = { Text("question,answer,explanation\n\"What is Synonmn?\",\"Exactly...\",\"Ex...\"") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (inputString.isNotBlank() && activeCatTargetId != 0L) {
                                    val isJson = inputString.trim().startsWith("[") || inputString.trim().startsWith("{")
                                    if (isJson) {
                                        scope.launch {
                                            viewModel.handleJsonImport(inputString, null).collect { success ->
                                                operationLog = if (success) "Successfully imported JSON data!" else "Failed. Validate JSON format."
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            viewModel.handleCsvImport(inputString, activeCatTargetId).collect { count ->
                                                operationLog = "Imported $count CSV card items successfully!"
                                            }
                                        }
                                    }
                                } else {
                                    operationLog = "Ensure target category selection is set."
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("validate_n_import_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Validate & Import")
                        }
                    }

                    if (operationLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(operationLog, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Bulk Exporter
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bulk Exporter Output", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Securely copies a serialized CSV backup to system clipboard.", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val csvOutput = StringBuilder("question,answer,explanation,tags\n")
                            items.forEach { card ->
                                val cleanQu = card.question.replace("\"", "\"\"")
                                val cleanAns = card.answer.replace("\"", "\"\"")
                                val cleanExpl = (card.explanation ?: "").replace("\"", "\"\"")
                                csvOutput.append("\"$cleanQu\",\"$cleanAns\",\"$cleanExpl\",\"${card.tags}\"\n")
                            }
                            clipboardManager.setText(AnnotatedString(csvOutput.toString()))
                            operationLog = "Exported ${items.size} items to Clipboard CSV!"
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("bulk_export_csv_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy CSV Backup (${items.size} Cards)")
                    }
                }
            }
        }
    }
}
