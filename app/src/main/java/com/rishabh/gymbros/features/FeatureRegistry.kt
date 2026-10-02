package com.rishabh.gymbros.features

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.rishabh.gymbros.navigation.AppDestination

enum class FeatureStatus {
    ACTIVE,
    COMING_SOON
}

data class FeatureDefinition(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val status: FeatureStatus,
    val destination: AppDestination? = null,
    val showInTabBar: Boolean = false,
    val tabLabel: String = title
)

data class BottomTabDefinition(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
)

object FeatureRegistry {
    val features: List<FeatureDefinition> = listOf(
        FeatureDefinition(
            id = "water",
            title = "Water",
            icon = Icons.Outlined.WaterDrop,
            status = FeatureStatus.ACTIVE,
            destination = AppDestination.WATER,
            showInTabBar = true
        ),
        FeatureDefinition(
            id = "workout",
            title = "Workout",
            icon = Icons.Outlined.FitnessCenter,
            status = FeatureStatus.ACTIVE,
            destination = AppDestination.TRAIN,
            showInTabBar = true,
            tabLabel = "Workout"
        ),
        FeatureDefinition(
            id = "nutrition",
            title = "Nutrition",
            icon = Icons.Outlined.Restaurant,
            status = FeatureStatus.COMING_SOON,
            destination = null
        ),
        FeatureDefinition(
            id = "toolbox",
            title = "Fitness Toolbox",
            icon = Icons.Outlined.Calculate,
            status = FeatureStatus.ACTIVE,
            destination = AppDestination.TOOLBOX
        ),
        FeatureDefinition(
            id = "checklist",
            title = "Gym Checklist",
            icon = Icons.Outlined.CheckCircle,
            status = FeatureStatus.ACTIVE,
            destination = AppDestination.CHECKLIST
        ),
        FeatureDefinition(
            id = "progress",
            title = "Progress",
            icon = Icons.Outlined.BarChart,
            status = FeatureStatus.ACTIVE,
            destination = AppDestination.PROGRESS
        )
    )

    val bottomTabs: List<BottomTabDefinition> = buildList {
        add(BottomTabDefinition(AppDestination.HOME, "Home", Icons.Outlined.Home))
        features
            .filter { it.showInTabBar && it.destination != null }
            .forEach { feature ->
                add(
                    BottomTabDefinition(
                        destination = requireNotNull(feature.destination),
                        label = feature.tabLabel,
                        icon = feature.icon
                    )
                )
            }
        add(BottomTabDefinition(AppDestination.YOU, "You", Icons.Outlined.Person))
    }
}
