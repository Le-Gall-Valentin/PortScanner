/**
 * The command-line TCP port scanner.
 *
 * <p>{@link ca.uqac.scanner.Main} wires the components and delegates to the command-line scanner, which
 * resolves the target and scans its well-known ports. The scanning engine lives in
 * {@link ca.uqac.scanner.core}, and its network adapters in {@link ca.uqac.scanner.net}.
 */
package ca.uqac.scanner;
