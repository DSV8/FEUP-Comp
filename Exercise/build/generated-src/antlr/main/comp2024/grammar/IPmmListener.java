// Generated from comp2024/grammar/IPmm.g4 by ANTLR 4.5.3

    package pt.up.fe.comp2024;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link IPmmParser}.
 */
public interface IPmmListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link IPmmParser#ips_set}.
	 * @param ctx the parse tree
	 */
	void enterIps_set(IPmmParser.Ips_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link IPmmParser#ips_set}.
	 * @param ctx the parse tree
	 */
	void exitIps_set(IPmmParser.Ips_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link IPmmParser#ip_adress}.
	 * @param ctx the parse tree
	 */
	void enterIp_adress(IPmmParser.Ip_adressContext ctx);
	/**
	 * Exit a parse tree produced by {@link IPmmParser#ip_adress}.
	 * @param ctx the parse tree
	 */
	void exitIp_adress(IPmmParser.Ip_adressContext ctx);
}