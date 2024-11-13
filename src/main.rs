use std::env;

use fondabots_lib::{
    affichan::Affichan,
    Bot
};
use maplit::hashmap;
use serenity::all::{ChannelId, GatewayIntents};

use ecrit::{
    fields::Status,
    fields::Type,
    Ecrit
};

mod ecrit;
mod commands;
pub type DataType = fondabots_lib::DataType<Ecrit>;

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    if let Some(token) = args.get(1) {
        match Bot::new(
            token.clone(),
            GatewayIntents::GUILD_MESSAGES | GatewayIntents::GUILD_MEMBERS,
            "./critibot.yml",
            commands::command_list(),
            vec![
                Affichan::new(ChannelId::new(1306257262360264714), Box::new(|ecrit| {
                    ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus
                })),
                Affichan::new(ChannelId::new(896361827884220467), Box::new(|ecrit| {
                    ecrit.status == Status::Inconnu || ecrit.status == Status::Infraction
                })),
                Affichan::new(ChannelId::new(896362452818747412), Box::new(|ecrit| {
                    ecrit.type_ == Type::Autre
                })),
            ],
            hashmap! {
                "logs" => 878917114474410004
            }
        ).await {
            Ok(mut bot) => if let Err(e) = bot.start().await {
                panic!("Erreur lors de l’exécution du bot: {e}");
            }
            Err(e) => panic!("Erreur lors du chargement du bot: {e}")
        }
    }
}