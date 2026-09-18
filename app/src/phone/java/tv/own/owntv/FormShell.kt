package tv.own.owntv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.own.owntv.features.shell.ShellArgs
import tv.own.owntv.phone.PhoneShell

/** نكهة الهاتف: قشرة اللمس (عموديّة، شريط سفليّ) — انظر [PhoneShell]. */
@Composable
fun FormShell(args: ShellArgs, modifier: Modifier = Modifier) {
    PhoneShell(args = args, modifier = modifier)
}
