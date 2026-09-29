/**
 * The port scanning engine, independent of any user interface and of any concrete network access.
 *
 * <p>A {@link ca.uqac.scanner.core.PortScanner} probes each port of a {@link ca.uqac.scanner.core.ScanConfig}
 * with a {@link ca.uqac.scanner.core.PortProbe} and reports the open ones, as they are found, to a
 * {@link ca.uqac.scanner.core.ScanListener}. A scan is cancelled by interrupting the thread that runs it.
 */
package ca.uqac.scanner.core;
