package com.greplog.commons;

import java.io.File;
import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.verification.ConsoleKnownHostsVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

public class RudimentaryPTY {

    public static void main(String... args)
            throws IOException {

        final SSHClient ssh = new SSHClient();

        final File khFile = new File(OpenSSHKnownHosts.detectSSHDir(), "known_hosts");
        ssh.addHostKeyVerifier(new ConsoleKnownHostsVerifier(khFile, System.console()));

        ssh.connect(Constants.H);
        try {

        	ssh.authPassword(Constants.U, Constants.P);
            final Session session = ssh.startSession();
            try {

                session.allocateDefaultPTY();

                final Shell shell = session.startShell();

                new StreamCopier(shell.getInputStream(), System.out)
                        .bufSize(shell.getLocalMaxPacketSize())
                        .spawn("stdout");

                new StreamCopier(shell.getErrorStream(), System.err)
                        .bufSize(shell.getLocalMaxPacketSize())
                        .spawn("stderr");

                // Now make System.in act as stdin. To exit, hit Ctrl+D (since that results in an EOF on System.in)
                // This is kinda messy because java only allows console input after you hit return
                // But this is just an example... a GUI app could implement a proper PTY
                new StreamCopier(System.in, shell.getOutputStream())
                        .bufSize(shell.getRemoteMaxPacketSize())
                        .copy();

            } finally {
                session.close();
            }

        } finally {
            ssh.disconnect();
        }
    }

}
