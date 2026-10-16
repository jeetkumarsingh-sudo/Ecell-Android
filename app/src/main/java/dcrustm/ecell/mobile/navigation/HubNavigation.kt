package dcrustm.ecell.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import dcrustm.ecell.mobile.ui.attendance.AttendanceScreen
import dcrustm.ecell.mobile.ui.attendance.HostMeetingScreen
import dcrustm.ecell.mobile.ui.hub.HubScreen
import dcrustm.ecell.mobile.ui.polls.CreatePollScreen
import dcrustm.ecell.mobile.ui.polls.PollsScreen
import dcrustm.ecell.mobile.ui.resources.AddResourceScreen
import dcrustm.ecell.mobile.ui.resources.ResourcesScreen
import dcrustm.ecell.mobile.ui.tasks.CreateTaskScreen
import dcrustm.ecell.mobile.ui.tasks.TasksScreen

/** Routes nested under the Hub tab. The graph itself is registered under [Screen.Hub]'s route. */
object HubRoutes {
    const val HOME = "hub_home"
    const val POLLS = "polls"
    const val CREATE_POLL = "polls/create"
    const val TASKS = "tasks"
    const val CREATE_TASK = "tasks/create"
    const val ATTENDANCE = "attendance"
    const val HOST_MEETING = "attendance/meeting/{meetingId}"

    const val RESOURCES = "resources"
    const val ADD_RESOURCE = "resources/add"

    fun hostMeeting(meetingId: String) = "attendance/meeting/$meetingId"
}

/**
 * Everything reachable from the Hub tab lives in one nested graph, so the Hub tab stays
 * highlighted (and keeps its back stack) while the member is inside any of its features.
 */
fun NavGraphBuilder.hubGraph(navController: NavHostController) {
    navigation(startDestination = HubRoutes.HOME, route = Screen.Hub.route) {
        composable(HubRoutes.HOME) {
            HubScreen(onOpen = { route -> navController.navigate(route) })
        }
        composable(HubRoutes.POLLS) {
            PollsScreen(
                onBack = { navController.popBackStack() },
                onCreatePoll = { navController.navigate(HubRoutes.CREATE_POLL) }
            )
        }
        composable(HubRoutes.CREATE_POLL) {
            CreatePollScreen(onBack = { navController.popBackStack() })
        }
        composable(HubRoutes.TASKS) {
            TasksScreen(
                onBack = { navController.popBackStack() },
                onCreateTask = { navController.navigate(HubRoutes.CREATE_TASK) }
            )
        }
        composable(HubRoutes.CREATE_TASK) {
            CreateTaskScreen(onBack = { navController.popBackStack() })
        }
        composable(HubRoutes.ATTENDANCE) {
            AttendanceScreen(
                onBack = { navController.popBackStack() },
                onOpenHostView = { id -> navController.navigate(HubRoutes.hostMeeting(id)) }
            )
        }
        composable(
            route = HubRoutes.HOST_MEETING,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) {
            HostMeetingScreen(onBack = { navController.popBackStack() })
        }
        composable(HubRoutes.RESOURCES) {
            ResourcesScreen(
                onBack = { navController.popBackStack() },
                onAddResource = { navController.navigate(HubRoutes.ADD_RESOURCE) }
            )
        }
        composable(HubRoutes.ADD_RESOURCE) {
            AddResourceScreen(onBack = { navController.popBackStack() })
        }
    }
}
